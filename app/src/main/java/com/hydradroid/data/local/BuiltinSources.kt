package com.hydradroid.data.local

import android.content.Context
import com.hydradroid.data.LibraryRepository

// Готовый набор источников загрузок из assets: добавляет недостающие по URL,
// а у существующих чинит ID (сервер принимает только короткие ID источников —
// неверные ID молча дают пустые варианты загрузок и игнор в фильтре каталога).
object BuiltinSources {

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
