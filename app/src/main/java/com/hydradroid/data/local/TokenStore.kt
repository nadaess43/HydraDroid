package com.hydradroid.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Порт auth-сессии оригинала: access/refresh токены в EncryptedSharedPreferences
// (аналог LevelDB userCredentials + handleUnauthorizedError → signout с чисткой).
object TokenStore {
    private const val FILE = "hydra_auth"

    private fun prefs(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            ctx, FILE, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(ctx: Context, access: String, refresh: String = "") {
        prefs(ctx).edit().putString("access", access).putString("refresh", refresh).apply()
    }

    fun getAccess(ctx: Context): String? = prefs(ctx).getString("access", null)

    fun clear(ctx: Context) { prefs(ctx).edit().clear().apply() }

    fun isSignedIn(ctx: Context): Boolean = !getAccess(ctx).isNullOrBlank()
}
