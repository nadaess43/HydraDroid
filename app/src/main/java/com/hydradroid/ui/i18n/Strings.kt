package com.hydradroid.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

/**
 * App language engine. Every user-visible literal goes through [Str.t] with the
 * ENGLISH text as the key; the [RU] table maps it to Russian. Unknown keys fall
 * back to English, so a missing translation never blanks the UI.
 *
 * Per-screen tables live in Tr*.kt (one NEW file per screen, no merge conflicts):
 * trMain, trHome, trCatalogue, trDetails, trDialog, trLibrary, trProfile, trComponents.
 * Placeholders: "{0}", "{1}" — replaced with [t] args in BOTH languages.
 */
class Str(val lang: String) {
    val isRu: Boolean get() = lang.startsWith("ru")

    fun t(en: String, vararg args: Any?): String {
        var s = if (isRu) RU[en] ?: en else en
        args.forEachIndexed { i, a -> s = s.replace("{$i}", a?.toString() ?: "") }
        return s
    }
}

val RU: Map<String, String> =
    trMain + trHome + trCatalogue + trDetails + trDialog + trLibrary + trProfile + trComponents

val LocalS = compositionLocalOf { Str("en") }

/** Current strings inside any @Composable. Recomposes automatically on language change. */
@Composable
@ReadOnlyComposable
fun ls(): Str = LocalS.current
