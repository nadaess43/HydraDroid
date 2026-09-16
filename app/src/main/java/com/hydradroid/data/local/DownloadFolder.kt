package com.hydradroid.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.first
import java.io.File

// Выбор папки загрузок: по умолчанию — приватная Downloads (всегда доступна),
// опционально — общая папка через SAF (ACTION_OPEN_DOCUMENT_TREE).
// Торрентам нужен реальный путь: SAF-дерево primary-хранилища маппим в путь,
// для остальных случаев честно откатываемся на приватную папку.
object DownloadFolder {

    fun defaultDir(ctx: Context): File {
        val base = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: ctx.filesDir
        val dir = File(base, "HydraDroid")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun savedTreeUri(ctx: Context): Uri? {
        val raw = ctx.prefs.data.first()[PrefKeys.DOWNLOAD_DIR_URI] ?: return null
        return try { Uri.parse(raw) } catch (_: Exception) { null }
    }

    suspend fun savedLabel(ctx: Context): String? =
        ctx.prefs.data.first()[PrefKeys.DOWNLOAD_DIR_LABEL]

    suspend fun saveTree(ctx: Context, uri: Uri, label: String) {
        try {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}
        ctx.prefs.edit {
            it[PrefKeys.DOWNLOAD_DIR_URI] = uri.toString()
            it[PrefKeys.DOWNLOAD_DIR_LABEL] = label
        }
    }

    suspend fun clearTree(ctx: Context) {
        ctx.prefs.edit {
            it.remove(PrefKeys.DOWNLOAD_DIR_URI)
            it.remove(PrefKeys.DOWNLOAD_DIR_LABEL)
        }
    }

    /** documentId "primary:Download/Hydra" → /storage/emulated/0/Download/Hydra */
    fun treeUriToRealPath(uri: Uri): File? {
        return try {
            val docId = when (uri.authority) {
                "com.android.externalstorage.documents" ->
                    android.provider.DocumentsContract.getTreeDocumentId(uri)
                else -> return null
            }
            val parts = docId.split(":", limit = 2)
            if (parts.size != 2) return null
            val (volume, rel) = parts
            val base = if (volume == "primary") Environment.getExternalStorageDirectory()
            else File("/storage/$volume")
            File(base, rel)
        } catch (_: Exception) { null }
    }

    /**
     * Папка для торрента/файла: если SAF-дерево маппится в реальный доступный для записи
     * путь — используем его (+подпапка при необходимости), иначе приватную папку.
     */
    suspend fun resolveRealDir(ctx: Context, subDir: String? = null): File {
        val tree = savedTreeUri(ctx)
        if (tree != null) {
            val real = treeUriToRealPath(tree)
            if (real != null) {
                val target = if (subDir != null) File(real, subDir) else real
                try {
                    if (!target.exists()) target.mkdirs()
                    if (target.canWrite()) return target
                } catch (_: Exception) {}
            }
        }
        val fallback = if (subDir != null) File(defaultDir(ctx), subDir) else defaultDir(ctx)
        if (!fallback.exists()) fallback.mkdirs()
        return fallback
    }

    /** Папка (SAF DocumentFile) для HTTP-загрузок: умеет писать и в общее дерево. */
    suspend fun resolveHttpTarget(ctx: Context, fileName: String, subDir: String? = null): HttpTarget {
        val tree = savedTreeUri(ctx)
        if (tree != null) {
            try {
                var dir = DocumentFile.fromTreeUri(ctx, tree)
                if (subDir != null) {
                    dir = dir?.findFile(subDir) ?: dir?.createDirectory(subDir)
                }
                if (dir != null && dir.canWrite()) return HttpTarget.Saf(dir, fileName)
            } catch (_: Exception) {}
        }
        val real = resolveRealDir(ctx, subDir)
        return HttpTarget.File(File(real, fileName))
    }

    fun displayPath(ctx: Context, label: String?): String =
        label ?: defaultDir(ctx).absolutePath

    fun freeBytes(dir: File): Long {
        return try {
            val stat = StatFs(dir.absolutePath)
            stat.availableBytes
        } catch (_: Exception) { -1L }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "—"
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = bytes.toDouble() / 1024
        var u = 0
        while (v >= 1024 && u < units.size - 1) { v /= 1024; u++ }
        return "%.1f %s".format(v, units[u])
    }

    fun formatSpeed(bytesPerSec: Long): String =
        if (bytesPerSec <= 0) "—" else "${formatBytes(bytesPerSec)}/s"

    fun formatEta(sec: Long): String {
        if (sec < 0) return "—"
        if (sec < 60) return "${sec}s"
        if (sec < 3600) return "${sec / 60}m ${sec % 60}s"
        return "${sec / 3600}h ${(sec % 3600) / 60}m"
    }
}

sealed interface HttpTarget {
    data class Saf(val dir: DocumentFile, val fileName: String) : HttpTarget
    data class File(val file: java.io.File) : HttpTarget
}
