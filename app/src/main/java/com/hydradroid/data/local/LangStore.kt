package com.hydradroid.data.local

import android.content.Context

/**
 * Synchronous mirror of the DataStore LANGUAGE pref for places without
 * composition (DownloadService notifications, error strings enqueued to Room).
 * Compose screens observe DataStore reactively; the service peeks here.
 * Single writer: [setLanguage] keeps both in sync.
 */
object LangStore {
    private const val FILE = "hydra_ui"
    private const val KEY = "ui_lang"

    fun peek(ctx: Context): String {
        return try {
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
                ?: ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).let {
                    // First run: adopt whatever DataStore already has is impossible
                    // synchronously — default to English.
                    "en"
                }
        } catch (_: Exception) {
            "en"
        }
    }

    fun persist(ctx: Context, code: String) {
        try {
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, code).apply()
        } catch (_: Exception) {}
    }
}
