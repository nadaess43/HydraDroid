package com.hydradroid.data.debrid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
            delay(2000)
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

    /** Unrestrict только через выбранный сервис (выбор в диалоге). */
    suspend fun unrestrictOne(service: String, rd: String, torbox: String, premiumize: String, link: String): DirectLink? {
        return when (service) {
            "rd" -> unrestrictRealDebrid(rd, link)
            "tb" -> unrestrictTorBox(torbox, link)
            "pm" -> unrestrictPremiumize(premiumize, link)
            else -> unrestrictFirst(rd, torbox, premiumize, link)
        }
    }

    // ── Удалённая закачка магнитов (какDownloader RealDebrid/TorBox/Premiumize
    // оригинала): сервер качает торрент сам, мы забираем прямую ссылку.
    // Спасает, когда DHT на телефоне не находит пиров.

    private fun getArray(url: String, headers: Map<String, String> = emptyMap()): org.json.JSONArray? {
        return try {
            val req = Request.Builder().url(url).apply {
                header("User-Agent", "HydraDroid")
                headers.forEach { (k, v) -> header(k, v) }
            }.build()
            client.newCall(req).execute().use {
                if (!it.isSuccessful) return null
                org.json.JSONArray((it.body ?: return null).string())
            }
        } catch (_: Exception) { null }
    }

    /** btih из магнита в hex (40 hex как есть, 32 base32 → hex). */
    fun hashFromMagnet(magnet: String): String? {
        val raw = Regex("btih:([a-zA-Z0-9]+)").find(magnet)?.groupValues?.getOrNull(1) ?: return null
        if (raw.length == 40 && raw.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return raw.lowercase()
        }
        if (raw.length == 32) {
            return try { base32ToHex(raw.uppercase()) } catch (_: Exception) { null }
        }
        return null
    }

    private fun base32ToHex(s: String): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var bits = 0
        var value = 0
        val out = ByteArray(20)
        var pos = 0
        for (c in s.trimEnd('=')) {
            val v = alphabet.indexOf(c)
            if (v < 0) throw IllegalArgumentException("base32")
            value = (value shl 5) or v
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out[pos++] = ((value shr bits) and 0xFF).toByte()
                if (pos == 20) break
            }
        }
        if (pos != 20) throw IllegalArgumentException("base32 len")
        return out.joinToString("") { "%02x".format(it) }
    }

    /** Real-Debrid: поиск в своих торрентах → addMagnet → selectFiles → unrestrict. */
    suspend fun rdMagnetToLink(token: String, magnet: String): DirectLink? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        val auth = mapOf("Authorization" to "Bearer $token")
        return@withContext try {
            val hash = hashFromMagnet(magnet)
            var id: String? = null
            if (hash != null) {
                val list = getArray("https://api.real-debrid.com/rest/1.0/torrents", auth)
                if (list != null) {
                    for (i in 0 until list.length()) {
                        val o = list.optJSONObject(i) ?: continue
                        if (o.optString("hash").lowercase() == hash) {
                            id = o.optString("id").ifBlank { null }
                            break
                        }
                    }
                }
            }
            if (id == null) {
                id = postForm("https://api.real-debrid.com/rest/1.0/torrents/addMagnet", mapOf("magnet" to magnet), auth)
                    ?.optString("id")?.ifBlank { null } ?: return@withContext null
            }
            // До ~90с ждём скачивание на стороне RD (кэшированные — мгновенно).
            repeat(45) {
                val info = get("https://api.real-debrid.com/rest/1.0/torrents/info/$id", auth)
                    ?: return@withContext null
                val status = info.optString("status")
                if (status == "waiting_files_selection") {
                    postForm(
                        "https://api.real-debrid.com/rest/1.0/torrents/selectFiles/$id",
                        mapOf("files" to "all"), auth
                    )
                } else if (status == "downloaded") {
                    val link = info.optJSONArray("links")?.optString(0)?.ifBlank { null }
                        ?: return@withContext null
                    return@withContext unrestrictRealDebrid(token, link)
                } else if (status == "error" || status == "dead") {
                    return@withContext null
                }
                kotlinx.coroutines.delay(2000)
            }
            null
        } catch (_: Exception) { null }
    }

    /** TorBox: mylist → createtorrent → poll → requestdl (zip). */
    suspend fun tbMagnetToLink(token: String, magnet: String): DirectLink? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        val enc = URLEncoder.encode(token, "UTF-8")
        val auth = mapOf("Authorization" to "Bearer $token")
        return@withContext try {
            val hash = hashFromMagnet(magnet)
            var id: Long? = null
            if (hash != null) {
                val list = get("https://api.torbox.app/v1/api/torrents/mylist?bypass_cache=true&limit=1000", auth)
                    ?.optJSONArray("data")
                if (list != null) {
                    for (i in 0 until list.length()) {
                        val o = list.optJSONObject(i) ?: continue
                        if (o.optString("hash").lowercase() == hash) {
                            id = o.optLong("id").takeIf { it != 0L }
                            break
                        }
                    }
                }
            }
            if (id == null) {
                id = postForm(
                    "https://api.torbox.app/v1/api/torrents/createtorrent",
                    mapOf("magnet" to magnet), auth
                )?.optJSONObject("data")?.optLong("torrent_id")?.takeIf { it != 0L }
                    ?: return@withContext null
            }
            repeat(30) {
                val info = get("https://api.torbox.app/v1/api/torrents/mylist?bypass_cache=true&id=$id", auth)
                    ?.optJSONArray("data")?.optJSONObject(0)
                if (info != null && info.optBoolean("download_finished", false) && info.optBoolean("download_present", false)) {
                    val dl = get(
                        "https://api.torbox.app/v1/api/torrents/requestdl?token=$enc&torrent_id=$id&zip_link=true",
                        auth
                    )
                    val url = (dl?.optString("data") ?: "").ifBlank { null } ?: return@withContext null
                    val name = info.optString("name").ifBlank { null }?.let { "$it.zip" }
                    return@withContext DirectLink(url, name, 0)
                }
                kotlinx.coroutines.delay(2000)
            }
            null
        } catch (_: Exception) { null }
    }

    /** Premiumize: directdl сразу; иначе transfer + короткий поллинг. */
    suspend fun pmMagnetToLink(apiKey: String, magnet: String): DirectLink? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        return@withContext try {
            val dd = postForm(
                "https://www.premiumize.me/api/transfer/directdl",
                mapOf("apikey" to apiKey, "src" to magnet)
            )
            val content = dd?.optJSONArray("content")
            if (content != null && content.length() == 1) {
                val o = content.optJSONObject(0)
                val url = java.net.URLDecoder.decode(o.optString("link"), "UTF-8").ifBlank { return@withContext null }
                return@withContext DirectLink(url, o.optString("path").substringAfterLast("/").ifBlank { null }, o.optLong("size", 0))
            }
            val tid = postForm(
                "https://www.premiumize.me/api/transfer/create",
                mapOf("apikey" to apiKey, "src" to magnet)
            )?.optString("id")?.ifBlank { null } ?: return@withContext null
            repeat(30) {
                val list = get("https://www.premiumize.me/api/transfer/list?apikey=${URLEncoder.encode(apiKey, "UTF-8")}")
                    ?.optJSONArray("transfers")
                if (list != null) {
                    for (i in 0 until list.length()) {
                        val o = list.optJSONObject(i) ?: continue
                        if (o.optString("id") == tid && (o.optString("status") == "finished" || o.optString("status") == "seeding")) {
                            val dd2 = postForm(
                                "https://www.premiumize.me/api/transfer/directdl",
                                mapOf("apikey" to apiKey, "src" to magnet)
                            )?.optJSONArray("content")?.optJSONObject(0)
                            val url = dd2?.optString("link")?.ifBlank { null }?.let {
                                java.net.URLDecoder.decode(it, "UTF-8")
                            } ?: return@withContext null
                            return@withContext DirectLink(url, null, 0)
                        }
                    }
                }
                kotlinx.coroutines.delay(2000)
            }
            null
        } catch (_: Exception) { null }
    }

    /** Магнит через первый готовый дебрид (параллельно, общий лимит ~100с). */
    suspend fun magnetViaFirst(rd: String, torbox: String, premiumize: String, magnet: String): DirectLink? {
        return try {
            withTimeoutOrNull(100000) {
                coroutineScope {
                    val r = async { rdMagnetToLink(rd, magnet) }
                    val t = async { tbMagnetToLink(torbox, magnet) }
                    val p = async { pmMagnetToLink(premiumize, magnet) }
                    r.await() ?: t.await() ?: p.await()
                }
            }
        } catch (_: Exception) { null }
    }

    /** Магнит через выбранный сервис. */
    suspend fun magnetViaOne(service: String, rd: String, torbox: String, premiumize: String, magnet: String): DirectLink? {
        return try {
            withTimeoutOrNull(100000) {
                when (service) {
                    RD_HOST -> rdMagnetToLink(rd, magnet)
                    TB_HOST -> tbMagnetToLink(torbox, magnet)
                    PM_HOST -> pmMagnetToLink(premiumize, magnet)
                    else -> magnetViaFirst(rd, torbox, premiumize, magnet)
                }
            }
        } catch (_: Exception) { null }
    }

    const val RD_HOST = "rd"
    const val TB_HOST = "tb"
    const val PM_HOST = "pm"
}
