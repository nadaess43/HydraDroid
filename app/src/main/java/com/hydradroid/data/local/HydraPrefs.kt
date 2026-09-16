package com.hydradroid.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Порт UserPreferences (~50 полей) + userPreferences.language + downloadSources
val Context.prefs by preferencesDataStore("hydra_prefs")

object PrefKeys {
    val LANGUAGE = stringPreferencesKey("language")
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val REAL_DEBRID_TOKEN = stringPreferencesKey("realdebrid_token")
    val TORBOX_TOKEN = stringPreferencesKey("torbox_token")
    val PREMIUMIZE_TOKEN = stringPreferencesKey("premiumize_token")
    val DOWNLOADS_PATH = stringPreferencesKey("downloads_path")
    val SHOW_PLAYABLE_ONLY = booleanPreferencesKey("show_playable_only")
    val LIBRARY_SORT = stringPreferencesKey("library_sort") // title_asc|recently_played|most_played|achievements
    val LIBRARY_VIEW = stringPreferencesKey("library_view") // grid|compact|large
    val SEARCH_HISTORY = stringSetPreferencesKey("search_history") // legacy, порядок не гарантирован
    val SEARCH_HISTORY_V2 = stringPreferencesKey("search_history_v2") // "\n"-список, свежие первые (макс 10)
    val FAVORITES_ONLY = booleanPreferencesKey("favorites_first")
    // ── Загрузки (порт pages/settings: settings-context-downloads) ──
    val DOWNLOAD_DIR_URI = stringPreferencesKey("download_dir_uri") // SAF tree URI
    val DOWNLOAD_DIR_LABEL = stringPreferencesKey("download_dir_label")
    val MAX_DOWNLOAD_SPEED = intPreferencesKey("max_download_kbps") // 0 = без лимита
    val MAX_UPLOAD_SPEED = intPreferencesKey("max_upload_kbps")     // 0 = без лимита
    val MAX_CONNECTIONS = intPreferencesKey("max_connections")     // 0 = по умолчанию (200)
    val SEED_RATIO = intPreferencesKey("seed_ratio_x100")          // 150 = 1.50, 0 = без лимита
    val SEED_TIME_MIN = intPreferencesKey("seed_time_min")         // 0 = без лимита
    val SEED_AFTER_COMPLETE = booleanPreferencesKey("seed_after_complete")
    val GLOBAL_TRACKERS = stringPreferencesKey("global_trackers")  // по одному URL на строку
    val DELETE_ARCHIVE_AFTER_EXTRACT = booleanPreferencesKey("delete_archive_after_extract")
}

fun languageFlow(ctx: Context) = ctx.prefs.data.map { it[PrefKeys.LANGUAGE] ?: "ru" }

suspend fun saveSearchHistory(ctx: Context, query: String) {
    val q = query.trim().replace("\n", " ")
    if (q.isBlank()) return
    ctx.prefs.edit { p ->
        val cur = (p[PrefKeys.SEARCH_HISTORY_V2] ?: "").lines()
            .map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        cur.remove(q)
        cur.add(0, q)
        p[PrefKeys.SEARCH_HISTORY_V2] = cur.take(10).joinToString("\n")
    }
}

fun searchHistoryFlow(ctx: Context): Flow<List<String>> =
    ctx.prefs.data.map {
        (it[PrefKeys.SEARCH_HISTORY_V2] ?: "").lines()
            .map { q -> q.trim() }.filter { q -> q.isNotBlank() }.take(10)
    }

suspend fun setPref(ctx: Context, key: Preferences.Key<String>, value: String) {
    ctx.prefs.edit { it[key] = value }
}

fun stringPrefFlow(ctx: Context, key: Preferences.Key<String>, default: String = ""): Flow<String> =
    ctx.prefs.data.map { it[key] ?: default }

suspend fun setIntPref(ctx: Context, key: Preferences.Key<Int>, value: Int) {
    ctx.prefs.edit { it[key] = value }
}

fun intPrefFlow(ctx: Context, key: Preferences.Key<Int>, default: Int = 0): Flow<Int> =
    ctx.prefs.data.map { it[key] ?: default }

suspend fun setBoolPref(ctx: Context, key: Preferences.Key<Boolean>, value: Boolean) {
    ctx.prefs.edit { it[key] = value }
}

fun boolPrefFlow(ctx: Context, key: Preferences.Key<Boolean>, default: Boolean = false): Flow<Boolean> =
    ctx.prefs.data.map { it[key] ?: default }
