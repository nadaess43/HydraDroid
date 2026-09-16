package com.hydradroid.data.archive

import com.github.junrar.Junrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.coroutineContext

// Распаковщик репаков (порт 7z/Ludusavi-части оригинала).
// zip/7z/tar/tgz — commons-compress с прогрессом, rar — junrar.
// ISO и многотомные .7z.001 честно помечаем неподдерживаемыми:
// их открывают внешние приложения/ПК-версия.
object ArchiveExtractor {

    enum class Kind(val label: String) {
        ZIP("ZIP"), SEVEN_Z("7z"), RAR("RAR"), TAR("TAR"), UNSUPPORTED("?")
    }

    fun detect(file: File): Kind {
        val n = file.name.lowercase()
        return when {
            n.endsWith(".zip") -> Kind.ZIP
            n.endsWith(".7z") && !n.matches(Regex(".*\\.7z\\.\\d+$")) -> Kind.SEVEN_Z
            n.endsWith(".rar") -> Kind.RAR
            n.endsWith(".tar") || n.endsWith(".tar.gz") || n.endsWith(".tgz") -> Kind.TAR
            n.endsWith(".iso") || n.endsWith(".7z.001") || n.endsWith(".part1.rar") -> Kind.UNSUPPORTED
            else -> Kind.UNSUPPORTED
        }
    }

    fun isArchive(fileName: String): Boolean =
        detect(File(fileName)) != Kind.UNSUPPORTED

    class UnsupportedException(message: String) : Exception(message)

    /**
     * Распаковка с прогрессом (doneBytes, totalBytes; total=-1 если неизвестен).
     * @return распакованные файлы
     */
    suspend fun extract(
        archive: File,
        destDir: File,
        onProgress: (done: Long, total: Long) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        if (!destDir.exists()) destDir.mkdirs()
        when (detect(archive)) {
            Kind.ZIP -> extractZip(archive, destDir, onProgress)
            Kind.SEVEN_Z -> extract7z(archive, destDir, onProgress)
            Kind.RAR -> extractRar(archive, destDir, onProgress)
            Kind.TAR -> extractTar(archive, destDir, onProgress)
            Kind.UNSUPPORTED -> throw UnsupportedException(
                "«${archive.name}»: ISO и многотомные архивы здесь не распаковываются — откройте внешним приложением."
            )
        }
    }

    private suspend fun extractZip(archive: File, dest: File, onProgress: (Long, Long) -> Unit): List<File> {
        val out = mutableListOf<File>()
        ZipFile.builder().setFile(archive).get().use { zip ->
            val entries = zip.entries.toList().filter { !it.isDirectory }
            val total = entries.sumOf { it.size.coerceAtLeast(0) }
            var done = 0L
            val buf = ByteArray(256 * 1024)
            for (e in entries) {
                coroutineContext.ensureActive()
                val target = safeTarget(dest, e.name)
                target.parentFile?.mkdirs()
                zip.getInputStream(e).use { ins ->
                    target.outputStream().use { os ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = ins.read(buf)
                            if (n < 0) break
                            os.write(buf, 0, n)
                            done += n
                        }
                    }
                }
                out += target
                onProgress(done, total)
            }
            onProgress(done, total)
        }
        return out
    }

    private suspend fun extract7z(archive: File, dest: File, onProgress: (Long, Long) -> Unit): List<File> {
        val out = mutableListOf<File>()
        SevenZFile.builder().setFile(archive).get().use { sevenZ ->
            val total = sevenZ.entries.sumOf { it.size.coerceAtLeast(0) }
            var done = 0L
            val buf = ByteArray(256 * 1024)
            while (true) {
                coroutineContext.ensureActive()
                val entry = sevenZ.nextEntry ?: break
                if (entry.isDirectory) continue
                val target = safeTarget(dest, entry.name)
                target.parentFile?.mkdirs()
                target.outputStream().use { os ->
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = sevenZ.read(buf)
                        if (n < 0) break
                        os.write(buf, 0, n)
                        done += n
                    }
                }
                out += target
                onProgress(done, total)
            }
            onProgress(done, total)
        }
        return out
    }

    private suspend fun extractRar(archive: File, dest: File, onProgress: (Long, Long) -> Unit): List<File> {
        // У junrar нет построчного прогресса: честный indeterminate (0 → 100 по факту).
        onProgress(0, -1)
        coroutineContext.ensureActive()
        val files = Junrar.extract(archive, dest)
        onProgress(1, 1)
        return files
    }

    private suspend fun extractTar(archive: File, dest: File, onProgress: (Long, Long) -> Unit): List<File> {
        val out = mutableListOf<File>()
        val raw: java.io.InputStream = if (archive.name.lowercase().endsWith(".gz") || archive.name.lowercase().endsWith(".tgz")) {
            GzipCompressorInputStream(BufferedInputStream(FileInputStream(archive)))
        } else BufferedInputStream(FileInputStream(archive))
        raw.use { ins ->
            TarArchiveInputStream(ins).use { tar ->
                var done = 0L
                val buf = ByteArray(256 * 1024)
                while (true) {
                    coroutineContext.ensureActive()
                    val entry = tar.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val target = safeTarget(dest, entry.name)
                    target.parentFile?.mkdirs()
                    target.outputStream().use { os ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = tar.read(buf)
                            if (n < 0) break
                            os.write(buf, 0, n)
                            done += n
                        }
                    }
                    out += target
                    onProgress(done, -1)
                }
                onProgress(done, -1)
            }
        }
        return out
    }

    /** Защита от Zip-Slip: запись только внутрь dest. */
    private fun safeTarget(dest: File, entryName: String): File {
        val clean = entryName.replace('\\', '/').trimStart('/')
        val target = File(dest, clean).canonicalFile
        require(target.path.startsWith(dest.canonicalPath + File.separator)) { "Опасный путь в архиве: $entryName" }
        return target
    }
}
