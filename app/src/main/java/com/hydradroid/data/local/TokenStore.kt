package com.hydradroid.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Auth session: access/refresh tokens in EncryptedSharedPreferences.
// Crash-safe: after reinstall/restore the keystore key may be gone while the
// encrypted file is restored via backup -> EncryptedSharedPreferences then throws
// on every access and kills the app on the splash screen. We catch that,
// wipe the corrupted file once and fall back to plain prefs so the app starts.
object TokenStore {
    private const val FILE = "hydra_auth"
    private const val FALLBACK = "hydra_auth_plain"

    private fun encrypted(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx, FILE, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun plain(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FALLBACK, Context.MODE_PRIVATE)

    private fun prefs(ctx: Context): SharedPreferences {
        return try {
            encrypted(ctx)
        } catch (_: Exception) {
            // Corrupted/restored encrypted file without its keystore key:
            // delete it so next launch can recreate it cleanly.
            try { ctx.deleteSharedPreferences(FILE) } catch (_: Exception) {}
            try { plain(ctx) } catch (_: Exception) {
                // Last resort: in-memory stub, never crashes the startup.
                return object : SharedPreferences by plain(ctx) {}
            }
        }
    }

    fun saveTokens(ctx: Context, access: String, refresh: String = "") {
        try {
            prefs(ctx).edit().putString("access", access).putString("refresh", refresh).apply()
        } catch (_: Exception) {
            try { ctx.deleteSharedPreferences(FILE) } catch (_: Exception) {}
            try {
                plain(ctx).edit().putString("access", access).putString("refresh", refresh).apply()
            } catch (_: Exception) {}
        }
    }

    fun getAccess(ctx: Context): String? {
        return try {
            prefs(ctx).getString("access", null)
        } catch (_: Exception) {
            try { ctx.deleteSharedPreferences(FILE) } catch (_: Exception) {}
            try { plain(ctx).getString("access", null) } catch (_: Exception) { null }
        }
    }

    fun clear(ctx: Context) {
        try { prefs(ctx).edit().clear().apply() } catch (_: Exception) {}
        try { plain(ctx).edit().clear().apply() } catch (_: Exception) {}
        try { ctx.deleteSharedPreferences(FILE) } catch (_: Exception) {}
    }

    fun isSignedIn(ctx: Context): Boolean {
        return try { !getAccess(ctx).isNullOrBlank() } catch (_: Exception) { false }
    }
}
