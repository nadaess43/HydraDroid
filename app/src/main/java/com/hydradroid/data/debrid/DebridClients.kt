package com.hydradroid.data.debrid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// Порт дебрид-интеграций оригинала (settings-real-debrid/torbox/premiumize).
// unrestrict превращает медленную/ограниченную ссылку репака в прямую быструю:
// её уже качает наш HttpDownloader с докачкой.
object Debrid {

    data class DirectLink(val url: String, val fileName: String?, val size: Long)

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun get(url: String, headers: Map<String, String> = emptyMap()): JSONObject? {
        return try {
            val req = Request.Builder().url(url).apply {
                header("User-Agent", "HydraDroid")
                headers.forEach { (k, v) -> header(k, v) }
            }.build()
            client.newCall(req).execute().use {
                if (!it.isSuccessful) return null
                JSONObject((it.body ?: return null).string())
            }
        } catch (_: Exception) { null }
    }

    private fun postForm(url: String, fields: Map<String, String>, headers: Map<String, String> = emptyMap()): JSONObject? {
        return try {
            val form = FormBody.Builder().apply { fields.forEach { (k, v) -> add(k, v) } }.build()
            val req = Request.Builder().url(url).post(form).apply {
                header("User-Agent", "HydraDroid")
                headers.forEach { (k, v) -> header(k, v) }
            }.build()
            client.newCall(req).execute().use {
                if (!it.isSuccessful) return null
                JSONObject((it.body ?: return null).string())
            }
        } catch (_: Exception) { null }
    }

    /** Real-Debrid POST /unrestrict/link */
    suspend fun unrestrictRealDebrid(token: String, link: String): DirectLink? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        val j = postForm(
            "https://api.real-debrid.com/rest/1.0/unrestrict/link",
            mapOf("link" to link),
            mapOf("Authorization" to "Bearer $token")
        ) ?: return@withContext null
        val url = j.optString("download").ifBlank { return@withContext null }
        DirectLink(url, j.optString("filename").ifBlank { null }, j.optLong("filesize", 0))
    }

    /** TorBox: webdownload (create → list → готовая ссылка). */
    suspend fun unrestrictTorBox(token: String, link: String): DirectLink? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        val enc = URLEncoder.encode(token, "UTF-8")
        postForm(
            "https://api.torbox.app/v1/api/webdl/createwebdownload?apikey=$enc",
            mapOf("url" to link)
        ) ?: return@withContext null
        // Опрашиваем список до готовности ссылки (до ~16с; общий таймаут режет сверху).
        repeat(8) {
            val list = get("https://api.torbox.app/v1/api/webdl/list?apikey=$enc&bypass_cache=true")
            val data = list?.optJSONArray("data")
            if (data != null) {
                for (i in 0 until data.length()) {
                    val o = data.optJSONObject(i) ?: continue
                    if (o.optString("original_url") == link || o.optString("url") == link) {
                        val dl = o.optString("downloadLink").ifBlank { o.optString("download_link") }
                        if (dl.isNotBlank() && (o.optBoolean("download_present", true))) {
                            return@withContext DirectLink(
                                dl, o.optString("name").ifBlank { null }, o.optLong("size", 0)
                            )
                        }
                    }
                }
            }
            Thread.sleep(2000)
        }
        null
    }

    /** Premiumize POST /transfer/directdl */
    suspend fun unrestrictPremiumize(apiKey: String, link: String): DirectLink? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val j = postForm(
            "https://www.premiumize.me/api/transfer/directdl",
            mapOf("apikey" to apiKey, "src" to link)
        ) ?: return@withContext null
        if (j.optString("status") != "success") return@withContext null
        val arr = j.optJSONArray("content") ?: return@withContext null
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val url = o.optString("stream_link").ifBlank { o.optString("link") }
            if (url.isNotBlank()) {
                return@withContext DirectLink(url, o.optString("path").substringAfterLast("/").ifBlank { null }, o.optLong("size", 0))
            }
        }
        null
    }

    /** Первая успешная расшифровка: все три сервиса гоняются ПАРАЛЛЕЛЬНО
     * (было последовательно — TorBox в одиночку мог висеть до 30с).
     * Приоритет при одновременной готовности: RD > TorBox > Premiumize. */
    suspend fun unrestrictFirst(rd: String, torbox: String, premiumize: String, link: String): DirectLink? {
        return try {
            withTimeoutOrNull(20000) {
                coroutineScope {
                    val rdD = async { unrestrictRealDebrid(rd, link) }
                    val tbD = async { unrestrictTorBox(torbox, link) }
                    val pmD = async { unrestrictPremiumize(premiumize, link) }
                    rdD.await() ?: tbD.await() ?: pmD.await()
                }
            }
        } catch (_: Exception) { null }
    }
}
