package com.hydradroid.data

import com.hydradroid.data.local.CollectionEntity
import com.hydradroid.data.local.CollectionGameCrossRef
import com.hydradroid.data.local.DownloadEntity
import com.hydradroid.data.local.DownloadSourceEntity
import com.hydradroid.data.local.HydraDatabase
import com.hydradroid.data.local.LibraryGameEntity
import com.hydradroid.data.model.CatalogueSearchResult
import com.hydradroid.data.model.GameRepack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

// Репозиторий библиотеки, загрузок, коллекций и источников (порт LevelDB-слоя оригинала).
class LibraryRepository(private val db: HydraDatabase) {
    private val dao get() = db.libraryDao()

    suspend fun getGames(sort: String, category: String, query: String, favoritesOnly: Boolean): List<LibraryGameEntity> =
        withContext(Dispatchers.IO) {
            val order = when (sort) {
                "title_asc" -> "title COLLATE NOCASE ASC"
                "title_desc" -> "title COLLATE NOCASE DESC"
                "most_played" -> "playTimeMs DESC"
                else -> "COALESCE(lastPlayed, addedAt) DESC"
            }
            val where = StringBuilder("1 = 1")
            val args = mutableListOf<Any>()
            if (favoritesOnly) where.append(" AND favorite = 1")
            if (category == "pc") where.append(" AND shop != 'launchbox'")
            if (category == "classics") where.append(" AND shop = 'launchbox'")
            if (query.isNotBlank()) {
                where.append(" AND title LIKE ? ESCAPE '\\'")
                // Аргументы биндятся (инъекции нет); экранируем только wildcards LIKE.
                args += "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
            }
            val sql = "SELECT * FROM library_games WHERE $where ORDER BY pinned DESC, $order"
            dao.getGamesRaw(androidx.sqlite.db.SimpleSQLiteQuery(sql, args.toTypedArray()))
        }

    suspend fun add(game: CatalogueSearchResult) = withContext(Dispatchers.IO) {
        dao.upsert(
            LibraryGameEntity(
                key = "${game.shop}:${game.objectId}", objectId = game.objectId, shop = game.shop,
                title = game.title, coverUrl = game.coverImageUrl ?: game.libraryImageUrl,
                iconUrl = null, heroUrl = game.libraryImageUrl
            )
        )
    }

    /** Локальная игра/файл (порт custom-games): скан папки, ручное добавление. */
    suspend fun addCustom(title: String, path: String) = withContext(Dispatchers.IO) {
        val key = "custom:${UUID.randomUUID()}"
        dao.upsert(LibraryGameEntity(key = key, objectId = key, shop = "custom", title = title, coverUrl = null, localPath = path))
    }

    suspend fun isAdded(shop: String, objectId: String): Boolean = withContext(Dispatchers.IO) {
        try { dao.exists("$shop:$objectId") } catch (_: Exception) { false }
    }

    suspend fun toggleFavorite(key: String) = withContext(Dispatchers.IO) {
        try { dao.flipFavorite(key) } catch (_: Exception) {}
    }

    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        dao.delete(key)
        dao.removeGameFromAllCollections(key)
    }

    // ── Очередь (инфо-записи из каталога; движок ведёт DownloadService) ──

    suspend fun enqueueRepack(shop: String, objectId: String, repack: GameRepack) = withContext(Dispatchers.IO) {
        dao.enqueue(
            DownloadEntity(
                id = repack.id, objectId = objectId, shop = shop, title = repack.title,
                fileSize = repack.fileSize, sourceName = repack.downloadSourceName,
                uri = repack.uris.firstOrNull() ?: ""
            )
        )
    }

    /** Полноценный трансфер: kind TORRENT|HTTP, magnet/uri, имя файла. */
    suspend fun startTransfer(
        shop: String, objectId: String, repack: GameRepack,
        kind: String, uri: String, fileName: String?
    ) = withContext(Dispatchers.IO) {
        val magnet = uri.takeIf { it.startsWith("magnet:") }
        dao.enqueue(
            DownloadEntity(
                id = repack.id, objectId = objectId, shop = shop, title = repack.title,
                fileSize = repack.fileSize, sourceName = repack.downloadSourceName,
                uri = uri, status = "queued", progress = 0f, kind = kind,
                magnet = magnet,
                torrentFilePath = if (kind == "TORRENT" && magnet == null && uri.endsWith(".torrent")) uri else null,
                fileName = fileName
            )
        )
    }

    suspend fun getQueue() = withContext(Dispatchers.IO) { dao.getQueue() }
    fun queueFlow() = dao.queueFlow()
    suspend fun getDownload(id: String) = withContext(Dispatchers.IO) { dao.getDownload(id) }
    suspend fun dequeue(id: String) = withContext(Dispatchers.IO) { dao.dequeue(id) }

    // ── Коллекции (порт collections-filter + create-collection-modal) ──

    suspend fun getCollections() = withContext(Dispatchers.IO) { dao.getCollections() }

    /** Счётчики всех коллекций одним запросом (было N+1). */
    suspend fun getCollectionCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        try { dao.getCollectionCounts().associate { it.id to it.c } } catch (_: Exception) { emptyMap() }
    }

    suspend fun createCollection(name: String): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        dao.upsertCollection(CollectionEntity(id, name.trim().take(60)))
        id
    }

    suspend fun renameCollection(id: String, name: String) = withContext(Dispatchers.IO) {
        dao.upsertCollection(CollectionEntity(id, name.trim().take(60)))
    }

    suspend fun deleteCollection(id: String) = withContext(Dispatchers.IO) {
        dao.clearCollection(id)
        dao.deleteCollection(id)
    }

    suspend fun getGameCollections(gameKey: String) = withContext(Dispatchers.IO) { dao.getGameCollections(gameKey) }

    suspend fun toggleInCollection(gameKey: String, collectionId: String) = withContext(Dispatchers.IO) {
        if (dao.getGameCollections(gameKey).contains(collectionId)) dao.removeFromCollection(gameKey, collectionId)
        else dao.addToCollection(CollectionGameCrossRef(gameKey, collectionId))
    }

    suspend fun getCollectionGames(id: String) = withContext(Dispatchers.IO) { dao.getCollectionGames(id) }

    // ── Источники загрузок (порт settings-download-sources) ──

    suspend fun getSources() = withContext(Dispatchers.IO) { dao.getSources() }

    suspend fun upsertSource(s: DownloadSourceEntity) = withContext(Dispatchers.IO) { dao.upsertSource(s) }

    suspend fun deleteSource(id: String) = withContext(Dispatchers.IO) { dao.deleteSource(id) }

    suspend fun enabledSourceIds(): List<String> = withContext(Dispatchers.IO) {
        dao.getSources().filter { it.enabled }.map { it.id }
    }

    /** Fingerprint включённых — для тела /catalogue/search (порт downloadSourceFingerprints). */
    suspend fun enabledSourceFingerprints(): List<String> = withContext(Dispatchers.IO) {
        dao.getSources().filter { it.enabled }.mapNotNull { it.fingerprint?.ifBlank { null } }
    }

    // ── Скан папки (порт scan-games-modal, упрощённо для телефона) ──
    // Ищем файлы игр/архивов и предлагаем добавить их в библиотеку как custom.

    data class ScannedFile(val name: String, val path: String, val size: Long)

    private val GAME_EXTS = setOf("iso", "zip", "7z", "rar", "tar", "gz", "wim", "nsp", "xci", "rvz", "cso", "wbfs")

    suspend fun scanFolder(dir: File, maxDepth: Int = 2): List<ScannedFile> = withContext(Dispatchers.IO) {
        val out = mutableListOf<ScannedFile>()
        fun walk(f: File, depth: Int) {
            if (depth > maxDepth) return
            val kids = try { f.listFiles() } catch (_: Exception) { null } ?: return
            for (k in kids) {
                if (k.isDirectory) walk(k, depth + 1)
                else if (k.extension.lowercase() in GAME_EXTS && k.length() > 0) {
                    out += ScannedFile(k.nameWithoutExtension, k.absolutePath, k.length())
                    if (out.size >= 200) return
                }
            }
        }
        if (dir.isDirectory) walk(dir, 0)
        out.sortedBy { it.name.lowercase() }
    }
}
