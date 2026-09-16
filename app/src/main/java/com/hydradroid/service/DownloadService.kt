package com.hydradroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.frostwire.jlibtorrent.TorrentStatus
import com.hydradroid.HydraDroidApp
import com.hydradroid.MainActivity
import com.hydradroid.R
import com.hydradroid.data.archive.ArchiveExtractor
import com.hydradroid.data.debrid.Debrid
import com.hydradroid.data.hosters.Hosters
import com.hydradroid.data.download.HttpDownloader
import com.hydradroid.data.local.DownloadEntity
import com.hydradroid.data.local.DownloadFolder
import com.hydradroid.data.local.HttpTarget
import com.hydradroid.data.local.LangStore
import com.hydradroid.data.local.PrefKeys
import com.hydradroid.data.local.intPrefFlow
import com.hydradroid.data.local.stringPrefFlow
import com.hydradroid.data.torrent.TorrentEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

// Foreground-сервис загрузок — порт download-manager + python-rpc оригинала.
// Ведёт торренты (libtorrent) и HTTP с докачкой, сидирует по лимитам,
// распаковывает архивы, держит постоянное уведомление с общей скоростью.
class DownloadService : Service() {

    companion object {
        const val CHANNEL = "hydra_downloads"
        const val FOREGROUND_ID = 41
        const val ACTION_START = "start"       // extra: id
        const val ACTION_PAUSE = "pause"       // extra: id
        const val ACTION_RESUME = "resume"     // extra: id
        const val ACTION_CANCEL = "cancel"     // extra: id, deleteFiles
        const val ACTION_EXTRACT = "extract"   // extra: id, archivePath
        const val EXTRA_ID = "id"

        /** Прогресс распаковок: id → (done, total). */
        private val _extractProgress = MutableStateFlow<Map<String, Pair<Long, Long>>>(emptyMap())
        val extractProgress: StateFlow<Map<String, Pair<Long, Long>>> = _extractProgress

        fun cmd(ctx: Context, action: String, id: String, deleteFiles: Boolean = false, archivePath: String? = null) {
            val i = Intent(ctx, DownloadService::class.java).setAction(action)
                .putExtra(EXTRA_ID, id).putExtra("deleteFiles", deleteFiles)
            if (archivePath != null) i.putExtra("archivePath", archivePath)
            ctx.startForegroundService(i)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpJobs = mutableMapOf<String, Job>()
    private val extractJobs = mutableMapOf<String, Job>()
    private var pollJob: Job? = null

    private val db get() = (applicationContext as HydraDroidApp).db
    private val notif get() = getSystemService(NotificationManager::class.java)

    /** No composition here — language comes from the LangStore mirror (kept in sync by setLanguage). */
    private fun isRu(): Boolean = try { LangStore.peek(this).startsWith("ru") } catch (_: Exception) { false }
    private fun tx(en: String, ru: String): String = if (isRu()) ru else en

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notif.createNotificationChannel(
            NotificationChannel(CHANNEL, tx("Downloads", "Загрузки"), NotificationManager.IMPORTANCE_LOW)
        )
        startForegroundCompat(statusNotification(tx("Download service running", "Сервис загрузок запущен"), null))
        pollJob = scope.launch { pollLoop() }
        // Перезапуск не завершённых трансферов после смерти процесса:
        // magnet/.torrent/HTTP-ссылки и прогресс лежат в Room — восстанавливаем без потерь.
        scope.launch { resumeActive() }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_ID, notification)
        }
    }

    /** Подъём активных трансферов после рестарта (движок пуст, Room — нет). */
    private suspend fun resumeActive() {
        val dao = db.libraryDao()
        val stale = try {
            dao.getQueue().filter {
                (it.kind == "TORRENT" || it.kind == "HTTP") &&
                    (it.status == "downloading" || it.status == "fetching" || it.status == "seeding")
            }
        } catch (_: Exception) { emptyList() }
        for (d in stale) {
            try {
                startTransfer(d.id)
                if (d.status == "seeding") {
                    // Сидирование продолжаем как было.
                }
            } catch (_: Exception) {
                try { dao.enqueue(d.copy(status = "paused", downSpeed = 0, upSpeed = 0)) } catch (_: Exception) {}
            }
            delay(500)
        }
        // Паузы восстанавливаем как паузы (движок пересоздался пустым).
        val paused = try {
            dao.getQueue().filter { it.kind == "TORRENT" && it.status == "paused" && it.infoHash != null }
        } catch (_: Exception) { emptyList() }
        for (d in paused) {
            try {
                startTransfer(d.id) // пересоздаём handle…
                d.infoHash?.let { TorrentEngine.pause(it) } // …и сразу ставим на паузу
                dao.enqueue(dao.getDownload(d.id)?.copy(status = "paused", downSpeed = 0, upSpeed = 0) ?: d)
            } catch (_: Exception) {}
            delay(500)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        try { TorrentEngine.stop() } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID) ?: return START_STICKY
        idlePolls = 0 // новая работа — сброс отсчёта тихого стопа
        when (intent.action) {
            ACTION_START -> scope.launch { startTransfer(id) }
            ACTION_PAUSE -> scope.launch { pauseTransfer(id) }
            ACTION_RESUME -> scope.launch { resumeTransfer(id) }
            ACTION_CANCEL -> scope.launch { cancelTransfer(id, intent.getBooleanExtra("deleteFiles", false)) }
            ACTION_EXTRACT -> scope.launch {
                extractArchive(id, intent.getStringExtra("archivePath") ?: return@launch)
            }
        }
        return START_STICKY
    }

    // ── Запуск ──

    private suspend fun startTransfer(id: String) {
        val d = db.libraryDao().getDownload(id) ?: return
        when (d.kind) {
            "TORRENT" -> startTorrent(d)
            "HTTP" -> startHttp(d)
        }
    }

    private suspend fun globalTrackers(): List<String> {
        val raw = applicationContext.prefsData(PrefKeys.GLOBAL_TRACKERS)
        val custom = raw.lines().map { it.trim() }.filter { it.startsWith("http") || it.startsWith("udp") }
        // Пустые настройки = голый DHT и вечное ожидание пиров. Из коробки — открытые трекеры.
        return if (custom.isNotEmpty()) custom else TorrentEngine.DEFAULT_TRACKERS
    }

    private suspend fun startTorrent(d: DownloadEntity) {
        val dao = db.libraryDao()
        try {
            TorrentEngine.start()
            val downKb = applicationContext.prefsInt(PrefKeys.MAX_DOWNLOAD_SPEED)
            val upKb = applicationContext.prefsInt(PrefKeys.MAX_UPLOAD_SPEED)
            val maxConn = applicationContext.prefsInt(PrefKeys.MAX_CONNECTIONS)
            TorrentEngine.applyLimits(downKb, upKb, maxConn)
        } catch (e: Exception) {
            dao.enqueue(d.copy(status = "error", error = tx("Engine: ", "Движок: ") + e.message))
            return
        }
        val saveDir = try {
            DownloadFolder.resolveRealDir(applicationContext, sanitize(d.title))
        } catch (e: Exception) {
            dao.enqueue(d.copy(status = "error", error = tx("Folder: ", "Папка: ") + e.message))
            return
        }
        dao.enqueue(d.copy(status = "fetching", saveDir = saveDir.absolutePath, error = null))
        // Удалённая закачка магнита через дебрид (как Downloader RealDebrid/TorBox
        // оригинала): DHT на телефоне часто не находит пиров вообще.
        val dlKind = d.downloader.ifBlank { "auto" }
        if (dlKind == Hosters.RD_REMOTE || dlKind == Hosters.TB_REMOTE || dlKind == Hosters.PM_REMOTE) {
            val magnet = d.magnet?.ifBlank { null } ?: d.uri.takeIf { it.startsWith("magnet:") }
            if (magnet == null) {
                dao.enqueue(d.copy(status = "error", error = tx("Magnet link: pick Torrent type", "Magnet-ссылка: выберите тип «Торрент»")))
                return
            }
            val rd = applicationContext.prefsData(PrefKeys.REAL_DEBRID_TOKEN)
            val tb = applicationContext.prefsData(PrefKeys.TORBOX_TOKEN)
            val pm = applicationContext.prefsData(PrefKeys.PREMIUMIZE_TOKEN)
            val link = Debrid.magnetViaOne(dlKind, rd, tb, pm, magnet)
            if (link == null) {
                val brand = Hosters.label(dlKind)
                dao.enqueue(d.copy(status = "error", error = tx("Not cached on {0}: try the torrent instead.", "Нет кэша на {0}: попробуйте торрент.").replace("{0}", brand)))
                return
            }
            val asHttp = d.copy(kind = "HTTP", fileName = link.fileName ?: d.fileName, downloader = "direct")
            dao.enqueue(asHttp)
            startHttp(asHttp, link.url)
            return
        }
        try {
            val trackers = globalTrackers()
            val hash = if (!d.magnet.isNullOrBlank()) {
                TorrentEngine.addMagnet(d.magnet, saveDir, trackers)
            } else if (!d.torrentFilePath.isNullOrBlank()) {
                val bytes = File(d.torrentFilePath).readBytes()
                TorrentEngine.addTorrentData(bytes, saveDir, trackers)
            } else if (d.uri.startsWith("magnet:")) {
                TorrentEngine.addMagnet(d.uri, saveDir, trackers)
            } else {
                // uri — ссылка на .torrent: скачиваем и отдаём движку.
                val bytes = fetchBytes(d.uri)
                // Bencode-словарь начинается с 'd': иначе это не торрент, а прямая ссылка.
                if (bytes.isEmpty() || bytes[0] != 'd'.code.toByte()) {
                    throw Exception(tx("Link doesn't serve a .torrent file — pick HTTP type for direct files", "Ссылка не отдаёт .torrent — для прямых файлов выберите тип «HTTP»"))
                }
                val tmp = File(cacheDir, "torrents/${d.id}.torrent")
                tmp.parentFile?.mkdirs()
                tmp.writeBytes(bytes)
                TorrentEngine.addTorrentData(bytes, saveDir, trackers).also {
                    dao.enqueue(dao.getDownload(d.id)?.copy(torrentFilePath = tmp.absolutePath) ?: d)
                }
            }
            // Применяем выбор файлов, если пользователь отметил до старта метаданных позже —
            // приоритеты ставятся отдельно через setFileSelection из UI.
            dao.enqueue(dao.getDownload(d.id)?.copy(status = "downloading", infoHash = hash, kind = "TORRENT") ?: d)
        } catch (e: Exception) {
            dao.enqueue(d.copy(status = "error", error = e.message?.take(300)))
        }
    }

    /** overrideUrl — уже резолвленная ссылка (дебрид-магнит): в базе храним оригинал. */
    private suspend fun startHttp(d: DownloadEntity, overrideUrl: String? = null) {
        val dao = db.libraryDao()
        if (httpJobs[d.id]?.isActive == true) return
        // Magnet по HTTP качать бессмысленно — сразу честная ошибка.
        if (d.uri.startsWith("magnet:")) {
            dao.enqueue(d.copy(status = "error", error = tx("Magnet link: pick Torrent type", "Magnet-ссылка: выберите тип «Торрент»")))
            return
        }
        dao.enqueue(d.copy(status = "downloading", error = null))
        val job = scope.launch {
            try {
                val rd = applicationContext.prefsData(PrefKeys.REAL_DEBRID_TOKEN)
                val tb = applicationContext.prefsData(PrefKeys.TORBOX_TOKEN)
                val pm = applicationContext.prefsData(PrefKeys.PREMIUMIZE_TOKEN)
                val dl = d.downloader.ifBlank { "auto" }
                // 1) Ссылка репака → рабочая ссылка: страницы хостеров резолвим,
                // иначе качали бы HTML или ловили 404.
                var base = overrideUrl ?: d.uri
                var baseName: String? = null
                var resolved = overrideUrl != null
                if (!resolved && dl != "direct") {
                    val hoster = if (dl == "auto") {
                        Hosters.optionsForUri(d.uri).firstOrNull { it != Hosters.DIRECT }
                    } else if (dl == "rd" || dl == "tb" || dl == "pm") {
                        null
                    } else dl
                    if (hoster != null) {
                        val r = try { Hosters.resolve(d.uri, hoster) } catch (_: Exception) { null }
                        if (r != null) {
                            base = r.url
                            baseName = r.fileName
                            resolved = true
                        } else if (dl != "auto") {
                            val err = tx("Could not resolve via {0}.", "Не получилось получить ссылку через {0}.")
                                .replace("{0}", Hosters.label(hoster))
                            dao.enqueue(dao.getDownload(d.id)?.copy(status = "error", error = err, downSpeed = 0) ?: d)
                            return@launch
                        }
                    }
                }
                // 2) Debrid: уже прямую ссылку не трогаем (лишние 20с и риск сломать).
                val direct = if (!resolved) {
                    when (dl) {
                        "rd", "tb", "pm" -> try {
                            Debrid.unrestrictOne(dl, rd, tb, pm, base)
                        } catch (_: Exception) { null }
                        else -> if (rd.isNotBlank() || tb.isNotBlank() || pm.isNotBlank()) {
                            try { Debrid.unrestrictFirst(rd, tb, pm, base) } catch (_: Exception) { null }
                        } else null
                    }
                } else null
                val url = direct?.url ?: base
                val rawName = d.fileName?.ifBlank { null }
                    ?: direct?.fileName
                    ?: baseName
                    ?: url.substringAfterLast("/").substringBefore("?").ifBlank { "${d.title}.bin" }
                val fileName = sanitizeFile(rawName)
                val cur = dao.getDownload(d.id) ?: d
                val target = DownloadFolder.resolveHttpTarget(applicationContext, fileName, sanitize(d.title))
                val resumeFrom = when (target) {
                    is HttpTarget.File -> if (target.file.exists()) target.file.length() else 0L
                    is HttpTarget.Saf -> target.dir.findFile(fileName)?.takeIf { it.exists() }?.length() ?: 0L
                }
                var lastEmit = 0L
                val total = if (target is HttpTarget.Saf) {
                    HttpDownloader.downloadToSaf(applicationContext, target.dir, fileName, url) { done, t ->
                        trackHttp(cur, done, t, fileName, target)
                        lastEmit = done
                    }
                } else {
                    val f = (target as HttpTarget.File).file
                    HttpDownloader.download(url, fileName, target, resumeFrom, cur.totalBytes) { done, t ->
                        trackHttp(cur, done, t, fileName, target)
                        lastEmit = done
                    }
                }
                val fin = dao.getDownload(d.id)
                if (fin != null && fin.status == "downloading") {
                    dao.enqueue(fin.copy(status = "complete", progress = 1f, doneBytes = total, totalBytes = total, downSpeed = 0))
                    notifyDone(fin.title, d.id)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                dao.enqueue(dao.getDownload(d.id)?.copy(status = "error", error = e.message?.take(300), downSpeed = 0) ?: d)
            } finally {
                httpJobs.remove(d.id)
            }
        }
        httpJobs[d.id] = job
    }

    private var lastHttpDbWrite = 0L
    private fun trackHttp(d: DownloadEntity, done: Long, total: Long, fileName: String, target: HttpTarget) {
        val now = System.currentTimeMillis()
        if (now - lastHttpDbWrite < 1000) return
        lastHttpDbWrite = now
        val saveDir = when (target) {
            is HttpTarget.File -> target.file.parent
            is HttpTarget.Saf -> null
        }
        scope.launch {
            db.libraryDao().enqueue(
                (db.libraryDao().getDownload(d.id) ?: d).copy(
                    doneBytes = done,
                    totalBytes = if (total > 0) total else d.totalBytes,
                    progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f,
                    fileName = fileName,
                    saveDir = saveDir ?: d.saveDir
                )
            )
        }
    }

    // ── Пауза / продолжение / отмена ──

    private suspend fun pauseTransfer(id: String) {
        val dao = db.libraryDao()
        val d = dao.getDownload(id) ?: return
        httpJobs[id]?.cancel()
        httpJobs.remove(id)
        if (d.kind == "TORRENT" && d.infoHash != null) TorrentEngine.pause(d.infoHash)
        dao.enqueue(d.copy(status = "paused", downSpeed = 0, upSpeed = 0))
    }

    private suspend fun resumeTransfer(id: String) {
        val dao = db.libraryDao()
        val d = dao.getDownload(id) ?: return
        if (d.kind == "TORRENT" && d.infoHash != null) {
            TorrentEngine.resume(d.infoHash)
            dao.enqueue(d.copy(status = "downloading", error = null))
        } else {
            startTransfer(id) // HTTP: новый Range-запрос с doneBytes; TORRENT без hash: полный рестарт
        }
    }

    private suspend fun cancelTransfer(id: String, deleteFiles: Boolean) {
        val dao = db.libraryDao()
        val d = dao.getDownload(id) ?: return
        httpJobs[id]?.cancel()
        httpJobs.remove(id)
        if (d.kind == "TORRENT" && d.infoHash != null) {
            val saveDir = d.saveDir?.let { File(it) }?.parentFile
            TorrentEngine.remove(d.infoHash, deleteFiles, saveDir, saveDir?.let { File(d.saveDir).name })
        }
        if (deleteFiles) {
            try {
                d.fileName?.let { fn ->
                    d.saveDir?.let { dir -> File(dir, fn).takeIf { it.exists() }?.delete() }
                    // SAF-цель: saveDir пуст, частичка лежит в общем дереве — чистим best-effort.
                    if (d.saveDir == null && d.kind == "HTTP") {
                        try {
                            val tree = DownloadFolder.savedTreeUri(applicationContext)
                            if (tree != null) {
                                androidx.documentfile.provider.DocumentFile.fromTreeUri(applicationContext, tree)
                                    ?.findFile(fn)?.takeIf { it.exists() }?.delete()
                            }
                        } catch (_: Exception) {}
                    }
                }
                d.saveDir?.let { dir ->
                    // Удаляем подпапку торрента/загрузки, но не корень Downloads.
                    val f = File(dir)
                    if (f.name != "HydraDroid" && f.name != "Download") f.deleteRecursively()
                }
            } catch (_: Exception) {}
        }
        dao.dequeue(id)
    }

    // ── Распаковка ──

    private suspend fun extractArchive(id: String, archivePath: String) {
        if (extractJobs[id]?.isActive == true) return
        val job = scope.launch {
            try {
                val archive = File(archivePath)
                val dest = File(archive.parentFile, archive.nameWithoutExtension)
                if (!dest.exists()) dest.mkdirs()
                _extractProgress.value = _extractProgress.value + (id to (0L to -1L))
                ArchiveExtractor.extract(archive, dest) { done, total ->
                    _extractProgress.value = _extractProgress.value + (id to (done to total))
                }
                _extractProgress.value = _extractProgress.value - id
                val deleteSrc = applicationContext.prefsBool(PrefKeys.DELETE_ARCHIVE_AFTER_EXTRACT, false)
                if (deleteSrc) { try { archive.delete() } catch (_: Exception) {} }
                notifyDone(tx("Extracted: ", "Распаковано: ") + archive.name, "extract_$id")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _extractProgress.value = _extractProgress.value - id
                notifyError(tx("Extraction failed: ", "Не распаковано: ") + e.message?.take(200))
            } finally {
                extractJobs.remove(id)
            }
        }
        extractJobs[id] = job
    }

    // ── Опрос движка ──

    private suspend fun CoroutineScope.pollLoop() {
        // Снапшоты последних записанных состояний: базу дёргаем только при
        // материальных изменениях, а не каждые 2 секунды (меньше IO и рекомпозиций).
        val last = mutableMapOf<String, Long>()
        var lastHeartbeat = 0L
        while (isActive) {
            try {
                val dao = db.libraryDao()
                val active = dao.getQueue().filter {
                    it.kind == "TORRENT" && (it.status == "downloading" || it.status == "fetching" || it.status == "seeding") && it.infoHash != null
                }
                for (d in active) {
                    // Свежий статус: пользователь мог поставить паузу/удалить между чтениями —
                    // иначе опрос воскрешал бы остановленные трансферы (гонка).
                    val fresh = dao.getDownload(d.id) ?: continue
                    if (fresh.status != "downloading" && fresh.status != "fetching" && fresh.status != "seeding") continue
                    val st = TorrentEngine.stats(fresh.infoHash!!)
                    if (st == null) continue
                    val ratioLimit = applicationContext.prefsInt(PrefKeys.SEED_RATIO) // x100
                    val timeLimitMin = applicationContext.prefsInt(PrefKeys.SEED_TIME_MIN)
                    val finished = st.state == TorrentStatus.State.FINISHED || st.state == TorrentStatus.State.SEEDING || st.progress >= 1f
                    val seedAfter = applicationContext.prefsBool(PrefKeys.SEED_AFTER_COMPLETE, true)
                    val newStatus = when {
                        !finished -> if (st.hasMetadata) "downloading" else "fetching"
                        !seedAfter -> "complete"
                        else -> {
                            val ratioOk = ratioLimit > 0 && st.totalWanted > 0 &&
                                st.totalUpload.toDouble() / st.totalWanted >= ratioLimit / 100.0
                            val timeOk = timeLimitMin > 0 && st.seedingDuration >= timeLimitMin * 60L
                            if ((ratioLimit > 0 || timeLimitMin > 0) && (ratioOk || timeOk)) "complete" else "seeding"
                        }
                    }
                    if (newStatus == "complete" && fresh.status != "complete") {
                        if (fresh.status == "seeding" || finished) TorrentEngine.pause(fresh.infoHash)
                        dao.enqueue(
                            fresh.copy(
                                status = "complete", progress = 1f, doneBytes = st.totalWanted,
                                totalBytes = st.totalWanted, uploadBytes = st.totalUpload,
                                downSpeed = 0, upSpeed = 0, etaSec = 0
                            )
                        )
                        notifyDone(fresh.title, fresh.id)
                    } else {
                        val eta = if (st.downSpeed > 0 && st.totalWanted > st.totalDone)
                            (st.totalWanted - st.totalDone) / st.downSpeed else -1L
                        // Сигнатура состояния: прогресс c шагом 0.2%, скорости в КБ, пиры, статус.
                        // Шум (байты тикают постоянно) в базу не пишем.
                        val sig = (((st.progress * 500).toInt() * 7919L +
                            st.downSpeed / 1024 * 104729L +
                            st.upSpeed / 1024 * 1299709L +
                            st.peers * 15485863L + st.seeds * 32452843L) * 31 +
                            newStatus.hashCode()) * 31 + (st.error?.hashCode() ?: 0)
                        val now = System.currentTimeMillis()
                        val heartbeat = now - lastHeartbeat > 15000
                        if (sig != last[d.id] || heartbeat) {
                            last[d.id] = sig
                            if (heartbeat) lastHeartbeat = now
                            dao.enqueue(
                                fresh.copy(
                                    status = newStatus, progress = st.progress.coerceIn(0f, 1f),
                                    doneBytes = st.totalDone, totalBytes = st.totalWanted,
                                    uploadBytes = st.totalUpload, downSpeed = st.downSpeed,
                                    upSpeed = st.upSpeed, etaSec = eta,
                                    peers = st.peers, seeds = st.seeds, error = st.error
                                )
                            )
                        }
                    }
                }
                updateStatusNotification()
            } catch (_: Exception) {}
            delay(2000)
        }
    }

    // ── Уведомления ──

    private fun contentIntent(): PendingIntent {
        val i = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun statusNotification(title: String, text: String?): Notification {
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle(title)
            .setContentText(text ?: tx("Torrents and HTTP downloads", "Торренты и HTTP-загрузки"))
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .build()
    }

    /** Счётчик пустых опросов: очередь пуста — уведомлению нечего висеть, гасим сервис. */
    private var idlePolls = 0

    private suspend fun updateStatusNotification() {
        val active = try {
            db.libraryDao().getQueue().filter { it.status == "downloading" || it.status == "fetching" || it.status == "seeding" }
        } catch (_: Exception) { emptyList() }
        if (active.isEmpty() && httpJobs.isEmpty() && extractJobs.isEmpty()) {
            idlePolls++
            if (idlePolls >= 3) {
                // ~6с тишины: снимаем foreground и останавливаемся, шторка чистая.
                // Новая команда (cmd) поднимет сервис заново через startForegroundService.
                try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) {}
                try { stopSelf() } catch (_: Exception) {}
            }
            return
        }
        idlePolls = 0
        val down = active.sumOf { it.downSpeed }
        val up = active.sumOf { it.upSpeed }
        val text = (if (isRu()) "Активно: " else "Active: ") + "${active.size} · ↓ ${DownloadFolder.formatSpeed(down)} · ↑ ${DownloadFolder.formatSpeed(up)}"
        notif.notify(FOREGROUND_ID, statusNotification("HydraDroid", text))
    }

    private fun notifyDone(title: String, key: String) {
        notif.notify(
            key.hashCode(),
            NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle(tx("Completed: ", "Завершено: ") + title)
                .setContentText(tx("Tap to open downloads", "Нажмите, чтобы открыть загрузки"))
                .setSmallIcon(R.drawable.ic_download)
                .setContentIntent(contentIntent())
                .setAutoCancel(true)
                .build()
        )
    }

    private fun notifyError(text: String) {
        notif.notify(
            text.hashCode(),
            NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle(tx("Error", "Ошибка"))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_download)
                .setContentIntent(contentIntent())
                .setAutoCancel(true)
                .build()
        )
    }

    private suspend fun fetchBytes(url: String): ByteArray {
        // Общий клиент, а не новый на каждый .torrent.
        val req = okhttp3.Request.Builder().url(url).header("User-Agent", "HydraDroid").build()
        HttpDownloader.client.newCall(req).execute().use {
            if (!it.isSuccessful) throw Exception("HTTP ${it.code}")
            return (it.body ?: throw Exception("Empty response")).bytes()
        }
    }

    private fun sanitize(name: String): String {
        val clean = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(80)
        return clean.ifBlank { "download" }
    }

    private fun sanitizeFile(name: String): String {
        val clean = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(120)
        return clean.ifBlank { "download.bin" }
    }

    private suspend fun Context.prefsData(key: androidx.datastore.preferences.core.Preferences.Key<String>): String {
        return stringPrefFlow(this, key, "").first()
    }

    private suspend fun Context.prefsInt(key: androidx.datastore.preferences.core.Preferences.Key<Int>): Int {
        return intPrefFlow(this, key, 0).first()
    }

    private suspend fun Context.prefsBool(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, def: Boolean): Boolean {
        return com.hydradroid.data.local.boolPrefFlow(this, key, def).first()
    }
}
