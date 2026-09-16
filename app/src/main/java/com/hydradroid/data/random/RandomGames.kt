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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

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

    /** Все 4 списка параллельно + дедуп по objectId (как reduce в оригинале). */
    suspend fun fetchList(cacheDir: File? = null): List<Steam250Game> = withContext(Dispatchers.IO) {
        // Файловый кэш на сутки — не дёргаем 4 страницы при каждом запуске.
        val cacheFile = cacheDir?.let { File(it, "steam250.json") }
        if (cacheFile != null && cacheFile.exists() &&
            System.currentTimeMillis() - cacheFile.lastModified() < 24 * 3600 * 1000L
        ) {
            try {
                val arr = JSONArray(cacheFile.readText())
                val cached = (0 until arr.length()).mapNotNull {
                    val o = arr.optJSONObject(it) ?: return@mapNotNull null
                    val id = o.optString("objectId")
                    if (id.isBlank()) null else Steam250Game(o.optString("title"), id)
                }
                if (cached.isNotEmpty()) return@withContext cached
            } catch (_: Exception) {}
        }
        val merged = LinkedHashMap<String, Steam250Game>()
        // Параллельно: 4 страницы грузятся разом, а не друг за другом.
        val pages = coroutineScope {
            paths().map { p -> async { fetchPage(p) } }.awaitAll()
        }
        for (page in pages) {
            for (g in page) {
                if (g.objectId !in merged) merged[g.objectId] = g
            }
        }
        val list = merged.values.toList()
        if (list.isNotEmpty() && cacheFile != null) {
            try {
                val arr = JSONArray()
                list.forEach { arr.put(JSONObject().put("title", it.title).put("objectId", it.objectId)) }
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(arr.toString())
            } catch (_: Exception) {}
        }
        list
    }
}

object RandomGameRoller {
    private val mutex = Mutex()
    private var games = mutableListOf<Steam250Game>()
    private var index = 0
    private var warmed = false

    /** Прогрев при старте приложения — первое нажатие мгновенное. */
    suspend fun warmup(cacheDir: File? = null) = mutex.withLock {
        if (!warmed) {
            warmed = true
            if (games.isEmpty()) {
                games = Steam250.fetchList(cacheDir).shuffled().toMutableList()
            }
        }
    }

    /** Порт getRandomGame 1:1: шаффл → выдача по кругу → ретасование в конце. */
    suspend fun next(cacheDir: File? = null): Steam250Game? = mutex.withLock {
        warmed = true
        if (games.isEmpty()) {
            games = Steam250.fetchList(cacheDir).shuffled().toMutableList()
        }
        if (games.isEmpty()) return null
        index += 1
        if (index == games.size) {
            index = 0
            games.shuffle()
        }
        games[index % games.size]
    }
}
