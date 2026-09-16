package com.hydradroid.data.random

import com.hydradroid.ui.cleanHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

// Порт main/services/steam-250.ts + main/events/catalogue/get-random-game.ts.
// «Удиви меня» в оригинале — это перетасованные списки Steam250
// (hidden_gems + текущий год + top250 + most_played), выдача по кругу без повторов:
// индекс растёт, в конце списка — новый шаффл. Пусто → "" (кнопка молча ничего не делает).
data class Steam250Game(val title: String, val objectId: String)

object Steam250 {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** 7 суток: списки меняются медленно, чаще дёргать сеть незачем. */
    private const val CACHE_TTL_MS = 7 * 24 * 3600 * 1000L

    // <a ... data-title="Terraria" ... href="https://club.steam250.com/app/105600" ...>
    private val TagRegex = Regex("<a\\b[^>]*>")
    private val TitleAttrRegex = Regex("data-title=\"([^\"]+)\"")
    private val AppIdRegex = Regex("/app/(\\d+)")

    private fun paths(): List<String> = listOf(
        "/hidden_gems",
        "/${Calendar.getInstance().get(Calendar.YEAR)}",
        "/top250",
        "/most_played"
    )

    private fun parse(html: String): List<Steam250Game> {
        val out = LinkedHashMap<String, Steam250Game>()
        for (tag in TagRegex.findAll(html)) {
            val t = tag.value
            if (!t.contains("data-title")) continue
            val title = TitleAttrRegex.find(t)?.groupValues?.getOrNull(1) ?: continue
            // href может идти до или после data-title — ищем по всему тегу.
            val id = AppIdRegex.find(t)?.groupValues?.getOrNull(1)
                ?: Regex("(?:store\\.steampowered\\.com/app|/app)/(\\d+)").find(t)?.groupValues?.getOrNull(1)
                ?: continue
            if (id.isNotBlank() && id !in out) {
                out[id] = Steam250Game(cleanHtml(title).ifBlank { title }, id)
            }
        }
        return out.values.toList()
    }

    private fun fetchPage(path: String): List<Steam250Game> {
        return try {
            val req = Request.Builder()
                .url("https://steam250.com$path")
                .header("User-Agent", "HydraDroid")
                .build()
            Steam250.client.newCall(req).execute().use {
                if (!it.isSuccessful) return emptyList()
                parse(it.body?.string() ?: "")
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun readCache(cacheFile: File): List<Steam250Game> {
        return try {
            if (!cacheFile.exists()) return emptyList()
            val arr = JSONArray(cacheFile.readText())
            (0 until arr.length()).mapNotNull {
                val o = arr.optJSONObject(it) ?: return@mapNotNull null
                val id = o.optString("objectId")
                if (id.isBlank()) null else Steam250Game(o.optString("title"), id)
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun isFresh(cacheFile: File): Boolean =
        System.currentTimeMillis() - cacheFile.lastModified() < CACHE_TTL_MS

    private fun writeCache(cacheFile: File, list: List<Steam250Game>) {
        try {
            val arr = JSONArray()
            list.forEach { arr.put(JSONObject().put("title", it.title).put("objectId", it.objectId)) }
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(arr.toString())
        } catch (_: Exception) {}
    }

    /** Все 4 списка параллельно + дедуп по objectId (как reduce в оригинале). */
    suspend fun fetchList(cacheDir: File? = null): List<Steam250Game> = withContext(Dispatchers.IO) {
        val cacheFile = cacheDir?.let { File(it, "steam250.json") }
        // Свежий кэш — сразу отдаём, сеть не трогаем.
        if (cacheFile != null && cacheFile.exists() && isFresh(cacheFile)) {
            val cached = readCache(cacheFile)
            if (cached.isNotEmpty()) return@withContext cached
        }
        // Параллельно: 4 страницы грузятся разом, а не друг за другом.
        val merged = LinkedHashMap<String, Steam250Game>()
        val pages = coroutineScope {
            paths().map { p -> async { fetchPage(p) } }.awaitAll()
        }
        for (page in pages) {
            for (g in page) {
                if (g.objectId !in merged) merged[g.objectId] = g
            }
        }
        val list = merged.values.toList()
        if (list.isNotEmpty()) {
            if (cacheFile != null) writeCache(cacheFile, list)
            return@withContext list
        }
        // Сеть отдала пустоту — лучше протухший кэш, чем ничего
        // (иначе КАЖДОЕ нажатие будет снова ждать сеть).
        if (cacheFile != null) return@withContext readCache(cacheFile)
        emptyList()
    }
}

object RandomGameRoller {
    private val mutex = Mutex()
    private var games = mutableListOf<Steam250Game>()
    private var index = 0
    private var warmed = false

    private fun pickLocked(): Steam250Game? {
        if (games.isEmpty()) return null
        index += 1
        if (index >= games.size) {
            index = 0
            games.shuffle()
        }
        return games[index % games.size]
    }

    /** Прогрев при старте: сеть — ВНЕ мьютекса, иначе нажатие ждёт прогрев. */
    suspend fun warmup(cacheDir: File? = null) {
        mutex.withLock { if (warmed && games.isNotEmpty()) return }
        val fresh = Steam250.fetchList(cacheDir)
        mutex.withLock {
            warmed = true
            if (games.isEmpty() && fresh.isNotEmpty()) {
                games = fresh.shuffled().toMutableList()
                index = 0
            }
        }
    }

    /** Порт getRandomGame 1:1: шаффл → выдача по кругу → ретасование в конце. */
    suspend fun next(cacheDir: File? = null): Steam250Game? {
        // Быстрый путь без сети.
        mutex.withLock {
            warmed = true
            pickLocked()?.let { return it }
        }
        // Пусто — грузим вне замка (параллельные нажатия не виснут друг на друге).
        val fresh = Steam250.fetchList(cacheDir)
        return mutex.withLock {
            if (games.isEmpty() && fresh.isNotEmpty()) {
                games = fresh.shuffled().toMutableList()
                index = 0
            }
            pickLocked()
        }
    }
}
