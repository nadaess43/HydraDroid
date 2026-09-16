package com.hydradroid.data.local

import android.content.Context
import com.hydradroid.data.LibraryRepository

// Набор источников загрузок из assets. По дефолту НЕ ставится: список пуст,
// пока пользователь не введёт код разблокировки в настройках источников.
object BuiltinSources {
    const val UNLOCK_CODE = "gaysicret228667"

    private fun readBundled(ctx: Context): List<Triple<String?, String, String?>> {
        val raw = try {
            ctx.assets.open("builtin_sources.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) { return emptyList() }
        val arr = try { org.json.JSONArray(raw) } catch (_: Exception) { return emptyList() }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val u = o.optString("url").trim()
            if (u.isBlank()) null else Triple(
                o.optString("id").trim().ifBlank { null }, u,
                o.optString("fingerprint").trim().ifBlank { null }
            )
        }
    }

    /** Разовая чистка бандла у тех, кто ставил старые сборки (свои URL не трогаем... кроме совпадающих). */
    suspend fun purgeBundled(ctx: Context, repo: LibraryRepository): Int {
        val bundled = readBundled(ctx)
        if (bundled.isEmpty()) return 0
        val ids = bundled.mapNotNull { it.first }.toSet()
        val urls = bundled.map { it.second }.toSet()
        val existing = try { repo.getSources() } catch (_: Exception) { emptyList() }
        var removed = 0
        for (s in existing) {
            if (s.id in ids || s.url in urls) {
                try { repo.deleteSource(s.id); removed++ } catch (_: Exception) {}
            }
        }
        return removed
    }

    suspend fun import(ctx: Context, repo: LibraryRepository): Int {
        val raw = try {
            ctx.assets.open("builtin_sources.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) { return 0 }
        val arr = try { org.json.JSONArray(raw) } catch (_: Exception) { return 0 }
        val existing = try { repo.getSources() } catch (_: Exception) { emptyList() }
        var changed = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val u = o.optString("url").trim()
            if (u.isBlank()) continue
            val id = o.optString("id").trim().ifBlank { null }
            val fp = o.optString("fingerprint").trim().ifBlank { null }
            val name = o.optString("name").trim().ifBlank { u }
            val same = existing.find { it.url == u }
            if (same != null && (id == null || same.id == id)) continue
            if (same != null) {
                try { repo.deleteSource(same.id) } catch (_: Exception) {}
            }
            try {
                repo.upsertSource(
                    DownloadSourceEntity(
                        id = id ?: "local-${u.hashCode()}",
                        name = name, url = u, fingerprint = fp
                    )
                )
                changed++
            } catch (_: Exception) {}
        }
        return changed
    }
}
