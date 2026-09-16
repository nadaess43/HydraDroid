package com.hydradroid

import android.app.Application
import androidx.room.Room
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
        db = Room.databaseBuilder(this, HydraDatabase::class.java, "hydra-db")
            .fallbackToDestructiveMigration() // dev-база v1→v2 без миграций
            .build()
        // Восстановление сессии как в оригинале (LevelDB userCredentials)
        HydraApiClient.accessToken = TokenStore.getAccess(this)
        // Порт handleUnauthorizedError: протухший токен → разлогин с чисткой.
        HydraApiClient.onUnauthorized = {
            try {
                TokenStore.clear(this)
            } catch (_: Exception) {}
            HydraApiClient.accessToken = null
        }
        // Готовые источники из коробки: без них сервер отдаёт пустые варианты загрузок.
        // Импорт идемпотентный — существующие записи не трогает, кривые ID чинит.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                com.hydradroid.data.local.BuiltinSources.import(
                    this@HydraDroidApp,
                    com.hydradroid.data.LibraryRepository(db)
                )
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
