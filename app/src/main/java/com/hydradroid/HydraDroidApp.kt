package com.hydradroid

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.hydradroid.data.local.HydraDatabase
import com.hydradroid.data.local.TokenStore
import com.hydradroid.data.remote.HydraApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HydraDroidApp : Application(), ImageLoaderFactory {
    lateinit var db: HydraDatabase
    override fun onCreate() {
        super.onCreate()
        // v4→v5: выбор сервиса загрузок (downloader), библиотеку НЕ трогаем.
        val migration45 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE download_queue ADD COLUMN downloader TEXT NOT NULL DEFAULT 'auto'")
            }
        }
        fun openDb() = Room.databaseBuilder(this, HydraDatabase::class.java, "hydra-db")
            .addMigrations(migration45)
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        db = try {
            openDb()
        } catch (_: Exception) {
            // Corrupted/restored DB file (backup downgrade): wipe once and recreate.
            try { deleteDatabase("hydra-db") } catch (_: Exception) {}
            openDb()
        }
        // Session restore off the main thread: keystore IO on cold start costs
        // 50-200ms of black screen. Catalogue calls are public anyway; authed
        // calls happen later, once the token is in place.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                HydraApiClient.accessToken = TokenStore.getAccess(this@HydraDroidApp)
            } catch (_: Exception) {
                HydraApiClient.accessToken = null
            }
        }
        // Порт handleUnauthorizedError: протухший токен → разлогин с чисткой.
        HydraApiClient.onUnauthorized = {
            try {
                TokenStore.clear(this)
            } catch (_: Exception) {}
            HydraApiClient.accessToken = null
        }
        // По дефолту источников нет (ни одной ссылки): бандл ставится только
        // по секретному коду в настройках. Старым установкам чистим бандл один раз.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val pf = getSharedPreferences("hydra_ui", MODE_PRIVATE)
                if (!pf.getBoolean("purged_bundled_v1", false)) {
                    com.hydradroid.data.local.BuiltinSources.purgeBundled(
                        this@HydraDroidApp,
                        com.hydradroid.data.LibraryRepository(db)
                    )
                    try { pf.edit().putBoolean("purged_bundled_v1", true).apply() } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    // Единый кэш картинок: память + диск, кроссфейд — ленты не дёргаются и не качают заново.
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
