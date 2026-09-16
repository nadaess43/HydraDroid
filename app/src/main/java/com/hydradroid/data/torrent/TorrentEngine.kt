package com.hydradroid.data.torrent

import com.frostwire.jlibtorrent.AnnounceEntry
import com.frostwire.jlibtorrent.AddTorrentParams
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentStatus
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// Порт торрент-движка оригинала (python-rpc + libtorrent на :5881).
// На Android тот же libtorrent 2.0 через jlibtorrent (FrostWire): DHT, magnet,
// .torrent, пауза/сидирование, приоритеты файлов, лимиты, трекеры.
// Все вызовы сессии идут через один поток: JNI-сессия не любит гонки.
object TorrentEngine {

    data class TorrentStats(
        val progress: Float,
        val downSpeed: Long,
        val upSpeed: Long,
        val peers: Int,
        val seeds: Int,
        val totalWanted: Long,
        val totalDone: Long,
        val totalUpload: Long,
        val seedingDuration: Long,
        val state: TorrentStatus.State,
        val name: String,
        val hasMetadata: Boolean,
        val error: String?
    )

    data class TorrentFileEntry(
        val index: Int,
        val path: String,
        val size: Long,
        val done: Long,
        val selected: Boolean
    )

    private val session = SessionManager()
    private val exec = Executors.newSingleThreadExecutor()
    @Volatile private var running = false

    val isRunning: Boolean get() = running

    private fun <T> call(timeoutSec: Long = 30, block: () -> T): T {
        return exec.submit(block).get(timeoutSec, TimeUnit.SECONDS)
    }

    /** Запуск сессии (DHT включён — magnet'ы резолвятся без трекеров). */
    fun start() {
        if (running) return
        call(60) { session.start() }
        running = true
    }

    fun stop() {
        if (!running) return
        try { call(30) { session.stop() } } catch (_: Exception) {}
        running = false
    }

    fun applyLimits(downKBs: Int, upKBs: Int, maxConnections: Int) {
        if (!running) return
        try {
            call {
                session.downloadRateLimit(if (downKBs > 0) downKBs * 1024 else 0)
                session.uploadRateLimit(if (upKBs > 0) upKBs * 1024 else 0)
                if (maxConnections > 0) session.maxConnections(maxConnections)
            }
        } catch (_: Exception) {}
    }

    /** Magnet → infoHash hex. Трекеры дописываем в URI (как FrostWire). */
    fun addMagnet(magnetUri: String, saveDir: File, extraTrackers: List<String>): String {
        val params = AddTorrentParams.parseMagnetUri(withTrackers(magnetUri, extraTrackers))
        params.savePath(saveDir.absolutePath)
        val hash = params.infoHashes.best.toHex()
        call {
            session.download(withTrackers(magnetUri, extraTrackers), saveDir,
                com.frostwire.jlibtorrent.swig.torrent_flags_t())
            // Страховка: пустые флаги не должны оставлять торрент на паузе.
            try { findHandle(hash, null)?.resume() } catch (_: Exception) {}
        }
        return hash
    }

    /** .torrent байты → infoHash hex (v1; для v2-only ищем по имени). */
    fun addTorrentData(data: ByteArray, saveDir: File, extraTrackers: List<String>): String {
        val ti = com.frostwire.jlibtorrent.TorrentInfo(data)
        val v1 = try { ti.infoHashV1().toHex() } catch (_: Exception) { "" }
        val handle = call(60) {
            session.download(ti, saveDir)
            val h = findHandle(v1.ifBlank { null }, ti.name())
            try { h?.resume() } catch (_: Exception) {}
            h
        }
        if (handle != null && extraTrackers.isNotEmpty()) {
            try { call { extraTrackers.forEach { handle.addTracker(AnnounceEntry(it)) } } } catch (_: Exception) {}
        }
        return v1.ifBlank { "name:${ti.name()}" }
    }

    fun pause(infoHashHex: String) {
        try { call { findHandle(infoHashHex, null)?.pause() } } catch (_: Exception) {}
    }

    fun resume(infoHashHex: String) {
        try { call { findHandle(infoHashHex, null)?.resume() } } catch (_: Exception) {}
    }

    /** Удаление из сессии; файлы удаляем сами (saveDir/подпапка), т.к. это надёжнее JNI-флагов. */
    fun remove(infoHashHex: String, deleteFiles: Boolean, saveDir: File?, subDir: String?) {
        try {
            call {
                findHandle(infoHashHex, null)?.let { session.remove(it) }
            }
        } catch (_: Exception) {}
        if (deleteFiles && saveDir != null) {
            try {
                val target = if (subDir != null) File(saveDir, subDir) else saveDir
                // Удаляем только подпапку торрента, никогда корень загрузок целиком без имени.
                if (subDir != null) target.deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    fun stats(infoHashHex: String): TorrentStats? {
        return try {
            call {
                val h = findHandle(infoHashHex, null) ?: return@call null
                val s = h.status()
                val err = s.errorCode()
                TorrentStats(
                    progress = s.progress(),
                    downSpeed = s.downloadRate().toLong(),
                    upSpeed = s.uploadRate().toLong(),
                    peers = s.numPeers(),
                    seeds = s.numSeeds(),
                    totalWanted = s.totalWanted(),
                    totalDone = s.totalDone(),
                    totalUpload = s.allTimeUpload(),
                    seedingDuration = try { s.seedingDuration() } catch (_: Exception) { 0L },
                    state = s.state(),
                    name = try { h.name() } catch (_: Exception) { s.name() },
                    hasMetadata = s.hasMetadata(),
                    // Пауза — источник правды Room (статус), у handle в этой версии нет isPaused.
                    error = if (err.value() != 0) err.message() else null
                )
            }
        } catch (_: Exception) { null }
    }

    fun listFiles(infoHashHex: String): List<TorrentFileEntry> {
        return try {
            call {
                val h = findHandle(infoHashHex, null) ?: return@call emptyList()
                val ti = try { h.torrentFile() } catch (_: Exception) { return@call emptyList() }
                if (!ti.isValid) return@call emptyList()
                val files = ti.files()
                val progress = try { h.fileProgress() } catch (_: Exception) { LongArray(files.numFiles()) }
                val prios = try { h.filePriorities() } catch (_: Exception) { null }
                (0 until files.numFiles()).map { i ->
                    TorrentFileEntry(
                        index = i,
                        path = try { files.filePath(i) } catch (_: Exception) { "file_$i" },
                        size = try { files.fileSize(i) } catch (_: Exception) { 0L },
                        done = progress.getOrElse(i) { 0L },
                        selected = prios?.getOrNull(i)?.let { it != Priority.IGNORE } ?: true
                    )
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    /** Выбор файлов: отмеченные — NORMAL, остальные — IGNORE (порт download-options модалки). */
    fun setFileSelection(infoHashHex: String, selected: Set<Int>) {
        try {
            call {
                val h = findHandle(infoHashHex, null) ?: return@call
                val n = try { h.torrentFile().files().numFiles() } catch (_: Exception) { return@call }
                h.prioritizeFiles(Array(n) { i -> if (i in selected) Priority.NORMAL else Priority.IGNORE })
            }
        } catch (_: Exception) {}
    }

    fun addTrackers(infoHashHex: String, trackers: List<String>) {
        if (trackers.isEmpty()) return
        try { call { findHandle(infoHashHex, null)?.let { h -> trackers.forEach { h.addTracker(AnnounceEntry(it)) } } } } catch (_: Exception) {}
    }

    fun sessionRates(): Pair<Long, Long> {
        return try { call { session.downloadRate() to session.uploadRate() } }
        catch (_: Exception) { 0L to 0L }
    }

    private fun findHandle(hexOrName: String?, name: String?): TorrentHandle? {
        if (!hexOrName.isNullOrBlank() && !hexOrName.startsWith("name:")) {
            try {
                session.find(Sha1Hash(hexOrName))?.let { if (it.isValid) return it }
            } catch (_: Exception) {}
        }
        // v2-only и фолбэк: ищем по имени среди handles.
        return try {
            val wanted = (name ?: hexOrName?.removePrefix("name:"))?.trim()
            session.torrentHandles.firstOrNull { h ->
                try {
                    h.isValid && (wanted.isNullOrBlank() || h.name() == wanted)
                } catch (_: Exception) { false }
            }
        } catch (_: Exception) { null }
    }

    private fun withTrackers(magnet: String, trackers: List<String>): String {
        if (trackers.isEmpty() || !magnet.startsWith("magnet:")) return magnet
        val sb = StringBuilder(magnet)
        trackers.forEach { tr ->
            if (!magnet.contains(URLEncoder.encode(tr, "UTF-8")) && !magnet.contains(tr)) {
                sb.append("&tr=").append(URLEncoder.encode(tr, "UTF-8"))
            }
        }
        return sb.toString()
    }

}
