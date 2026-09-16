package com.hydradroid.data.local

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

// Порт LevelDB games/downloadSources/collections/shopAssetsCache (TTL 8ч как getGameAssets)
@Entity(tableName = "library_games")
data class LibraryGameEntity(
    @PrimaryKey val key: String, // "$shop:$objectId"
    val objectId: String, val shop: String, val title: String,
    val coverUrl: String? = null, val iconUrl: String? = null,
    val heroUrl: String? = null, val logoUrl: String? = null,
    val playTimeMs: Long = 0, val lastPlayed: Long? = null,
    val favorite: Boolean = false, val pinned: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val localPath: String? = null // custom: путь к локальному ISO/архиву (скан папки)
)

@Entity(tableName = "collections")
data class CollectionEntity(@PrimaryKey val id: String, val name: String)

// Порт коллекций оригинала: игра может лежать в нескольких коллекциях.
@Entity(tableName = "collection_games", primaryKeys = ["gameKey", "collectionId"])
data class CollectionGameCrossRef(val gameKey: String, val collectionId: String)

data class CollectionCount(val id: String, val c: Int)

@Entity(tableName = "assets_cache")
data class AssetsCacheEntity(@PrimaryKey val key: String, val json: String, val cachedAt: Long)

// Порт источников загрузок (settings-download-sources): участвуют в фильтре каталога.
@Entity(tableName = "download_sources")
data class DownloadSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val fingerprint: String? = null,
    val enabled: Boolean = true
)

// Активная загрузка: торрент (libtorrent) или HTTP с докачкой.
// kind: INFO (только запись из каталога) | TORRENT | HTTP
// status: queued|fetching|downloading|paused|seeding|complete|error
@Entity(tableName = "download_queue")
data class DownloadEntity(
    @PrimaryKey val id: String, // repack id
    val objectId: String, val shop: String, val title: String,
    val fileSize: String? = null, val sourceName: String,
    val uri: String, // magnet / .torrent-URL / прямая HTTP-ссылка (оригинал, резолв не храним — ссылки протухают)
    val downloader: String = "auto", // auto|torrent|direct|gofile|pixeldrain|mediafire|fuckingfast|rootz|datanodes|vikingfile|archiveorg|rd|tb|pm|rd_remote|tb_remote|pm_remote
    val status: String = "queued",
    val progress: Float = 0f,
    val kind: String = "INFO",
    val magnet: String? = null,
    val torrentFilePath: String? = null, // скачанный .torrent для движка
    val saveDir: String? = null,         // реальный путь папки назначения
    val fileName: String? = null,        // имя для HTTP-загрузок
    val totalBytes: Long = 0L,
    val doneBytes: Long = 0L,
    val uploadBytes: Long = 0L,
    val downSpeed: Long = 0L,            // байт/с
    val upSpeed: Long = 0L,              // байт/с
    val etaSec: Long = -1L,
    val peers: Int = 0,
    val seeds: Int = 0,
    val infoHash: String? = null,        // hex, ключ торрента в движке
    val error: String? = null,
    val queuedAt: Long = System.currentTimeMillis()
)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_games ORDER BY pinned DESC, lastPlayed DESC")
    suspend fun getAll(): List<LibraryGameEntity>
    // Фильтры одним SQL вместо загрузки всей таблицы + N+1 счётчиков в коде.
    @RawQuery(observedEntities = [LibraryGameEntity::class])
    suspend fun getGamesRaw(query: SupportSQLiteQuery): List<LibraryGameEntity>
    @Query("SELECT collectionId AS id, COUNT(*) AS c FROM collection_games GROUP BY collectionId")
    suspend fun getCollectionCounts(): List<CollectionCount>
    @Query("SELECT * FROM library_games WHERE favorite = 1")
    suspend fun getFavorites(): List<LibraryGameEntity>
    @Upsert suspend fun upsert(game: LibraryGameEntity)
    @Query("SELECT EXISTS(SELECT 1 FROM library_games WHERE `key` = :key)")
    suspend fun exists(key: String): Boolean
    @Query("UPDATE library_games SET favorite = NOT favorite WHERE `key` = :key")
    suspend fun flipFavorite(key: String)
    @Query("DELETE FROM library_games WHERE `key` = :key")
    suspend fun delete(key: String)
    @Query("SELECT * FROM collections")
    suspend fun getCollections(): List<CollectionEntity>
    @Upsert suspend fun upsertCollection(c: CollectionEntity)
    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)
    @Query("DELETE FROM collection_games WHERE collectionId = :id")
    suspend fun clearCollection(id: String)
    @Query("DELETE FROM collection_games WHERE gameKey = :gameKey")
    suspend fun removeGameFromAllCollections(gameKey: String)
    @Upsert suspend fun addToCollection(ref: CollectionGameCrossRef)
    @Query("DELETE FROM collection_games WHERE gameKey = :gameKey AND collectionId = :collectionId")
    suspend fun removeFromCollection(gameKey: String, collectionId: String)
    @Query("SELECT collectionId FROM collection_games WHERE gameKey = :gameKey")
    suspend fun getGameCollections(gameKey: String): List<String>
    @Query("SELECT * FROM library_games WHERE `key` IN (SELECT gameKey FROM collection_games WHERE collectionId = :id)")
    suspend fun getCollectionGames(id: String): List<LibraryGameEntity>
    @Query("SELECT * FROM assets_cache WHERE `key` = :key")
    suspend fun getAssets(key: String): AssetsCacheEntity?
    @Upsert suspend fun putAssets(e: AssetsCacheEntity)
    @Query("SELECT * FROM download_queue ORDER BY queuedAt DESC")
    suspend fun getQueue(): List<DownloadEntity>
    // Реактивная очередь: UI обновляется только по факту изменений в базе,
    // а не опросом по таймеру (нагрузка на базу и рекомпозиции → в разы меньше).
    @Query("SELECT * FROM download_queue ORDER BY queuedAt DESC")
    fun queueFlow(): Flow<List<DownloadEntity>>
    @Query("SELECT * FROM download_queue WHERE id = :id")
    suspend fun getDownload(id: String): DownloadEntity?
    @Upsert suspend fun enqueue(e: DownloadEntity)
    @Query("DELETE FROM download_queue WHERE id = :id")
    suspend fun dequeue(id: String)
    @Query("UPDATE download_queue SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Float)
    @Query("SELECT * FROM download_sources ORDER BY name")
    suspend fun getSources(): List<DownloadSourceEntity>
    @Upsert suspend fun upsertSource(s: DownloadSourceEntity)
    @Query("DELETE FROM download_sources WHERE id = :id")
    suspend fun deleteSource(id: String)
}

@Database(
    entities = [LibraryGameEntity::class, CollectionEntity::class, CollectionGameCrossRef::class, AssetsCacheEntity::class, DownloadEntity::class, DownloadSourceEntity::class],
    version = 5, exportSchema = false
)
abstract class HydraDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
