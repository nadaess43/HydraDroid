package com.hydradroid.data.download

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import com.hydradroid.data.local.HttpTarget
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

// HTTP-загрузчик с докачкой (порт HTTP-части загрузок оригинала).
// Пишет и в обычную папку, и в SAF-дерево (общие Downloads).
// Пауза = отмена корутины, прогресс хранится в Room, продолжение — Range-запросом.
object HttpDownloader {

    // Общий клиент (переиспользуется и сервисом для .torrent).
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // 30с на stall: пауза/отмена вступают быстро (проверка ensureActive между чанками),
        // медленные дебрид-ссылки при этом не рвутся.
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    class HttpException(message: String) : Exception(message)

    /**
     * @param resumeFrom уже скачано байт (Range: bytes=resumeFrom-)
     * @param onProgress вызывается примерно раз в 500мс (doneBytes, totalBytes)
     * @return итоговый размер файла
     */
    suspend fun download(
        url: String,
        fileName: String,
        target: HttpTarget,
        resumeFrom: Long,
        expectedTotal: Long,
        extraHeaders: Map<String, String> = emptyMap(),
        onProgress: (done: Long, total: Long) -> Unit
    ): Long {
        val req = Request.Builder().url(url).apply {
            if (resumeFrom > 0) header("Range", "bytes=$resumeFrom-")
            header("User-Agent", "HydraDroid")
            extraHeaders.forEach { (k, v) -> header(k, v) }
        }.build()

        val resp = client.newCall(req).execute()
        resp.use {
            if (!it.isSuccessful) throw HttpException("HTTP ${it.code}")
            val body = it.body ?: throw HttpException("Empty response")
            val partial = it.code == 206
            val start = if (partial) resumeFrom else 0L
            val total = (body.contentLength().takeIf { l -> l > 0 }?.let { l -> l + start }
                ?: expectedTotal.takeIf { t -> t > 0 } ?: -1L)

            when (target) {
                is HttpTarget.File -> writeToFile(target.file, body.byteStream(), start, total, onProgress)
                // SAF пишется через downloadToSaf(): DocumentFile не хранит ContentResolver.
                is HttpTarget.Saf -> throw HttpException("Internal target error")
            }
            return total
        }
    }

    private suspend fun writeToFile(
        file: File, input: java.io.InputStream, start: Long, total: Long,
        onProgress: (Long, Long) -> Unit
    ) {
        file.parentFile?.mkdirs()
        RandomAccessFile(file, "rw").use { raf ->
            if (start == 0L) raf.setLength(0)
            raf.seek(start)
            pump(input, rafChannelWriter(raf), start, total, onProgress)
        }
    }

    private fun rafChannelWriter(raf: RandomAccessFile): (ByteArray, Int) -> Unit {
        return { buf, n -> raf.write(buf, 0, n) }
    }

    /**
     * SAF-вариант с резолвером (DocumentFile сам resolver не хранит).
     */
    suspend fun downloadToSaf(
        ctx: Context,
        dir: DocumentFile,
        fileName: String,
        url: String,
        extraHeaders: Map<String, String> = emptyMap(),
        onProgress: (done: Long, total: Long) -> Unit
    ): Long {
        val existing = dir.findFile(fileName)
        val resumeFrom = existing?.takeIf { it.exists() }?.length() ?: 0L
        val req = Request.Builder().url(url).apply {
            if (resumeFrom > 0) header("Range", "bytes=$resumeFrom-")
            header("User-Agent", "HydraDroid")
            extraHeaders.forEach { (k, v) -> header(k, v) }
        }.build()

        val resp = client.newCall(req).execute()
        resp.use {
            if (!it.isSuccessful) throw HttpException("HTTP ${it.code}")
            val body = it.body ?: throw HttpException("Empty response")
            val partial = it.code == 206
            val start = if (partial) resumeFrom else 0L
            val file = if (start == 0L) {
                existing?.delete()
                dir.createFile("application/octet-stream", fileName)
                    ?: throw HttpException("Could not create file in the chosen folder")
            } else existing ?: throw HttpException("File not found")
            val total = body.contentLength().takeIf { l -> l > 0 }?.let { l -> l + start } ?: -1L
            ctx.contentResolver.openFileDescriptor(file.uri, "wa")?.use { pfd ->
                ParcelFileDescriptor.AutoCloseOutputStream(pfd).use { out ->
                    val channel = out.channel
                    channel.position(start)
                    pump(body.byteStream(), { buf, n -> out.write(buf, 0, n) }, start, total, onProgress)
                }
            } ?: throw HttpException("Could not open file for writing")
            return total
        }
    }

    private suspend fun pump(
        input: java.io.InputStream, write: (ByteArray, Int) -> Unit,
        start: Long, total: Long, onProgress: (done: Long, total: Long) -> Unit
    ) {
        val buf = ByteArray(256 * 1024)
        var done = start
        var lastEmit = System.currentTimeMillis()
        input.use { ins ->
            while (true) {
                coroutineContext.ensureActive()
                val n = ins.read(buf)
                if (n < 0) break
                write(buf, n)
                done += n
                val now = System.currentTimeMillis()
                if (now - lastEmit > 500) {
                    lastEmit = now
                    onProgress(done, total)
                }
            }
        }
        onProgress(done, total)
    }
}
