package com.hydradroid.data.hosters

import com.hydradroid.data.debrid.Debrid
import com.hydradroid.data.remote.HydraApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Прямые резолверы хостеров — порт services-hosters и shared-archive-org оригинала.
 * Проблема: ссылки репаков часто ведут на СТРАНИЦУ хостера (gofile/pixeldrain/...),
 * а не на файл. Прямое скачивание такой ссылки даёт HTML или 404.
 * resolve() превращает её в прямую ссылку на файл, как download-manager оригинала.
 */
object Hosters {
    const val TORRENT = "torrent"
    const val DIRECT = "direct"
    const val AUTO = "auto"
    const val GOFILE = "gofile"
    const val PIXELDRAIN = "pixeldrain"
    const val DATANODES = "datanodes"
    const val MEDIAFIRE = "mediafire"
    const val FUCKINGFAST = "fuckingfast"
    const val VIKINGFILE = "vikingfile"
    const val ROOTZ = "rootz"
    const val ARCHIVEORG = "archiveorg"
    const val RD_REMOTE = "rd_remote"
    const val TB_REMOTE = "tb_remote"
    const val PM_REMOTE = "pm_remote"

    fun label(id: String): String = when (id) {
        TORRENT -> "Torrent"
        DIRECT -> "Direct link"
        GOFILE -> "Gofile"
        PIXELDRAIN -> "PixelDrain"
        DATANODES -> "Datanodes"
        MEDIAFIRE -> "Mediafire"
        FUCKINGFAST -> "FuckingFast"
        VIKINGFILE -> "VikingFile"
        ROOTZ -> "Rootz"
        ARCHIVEORG -> "Archive.org"
        RD_REMOTE -> "Real-Debrid"
        TB_REMOTE -> "TorBox"
        PM_REMOTE -> "Premiumize"
        else -> id
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val BROWSER_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    private const val FF_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0"

    /** Порт getDownloadersForUri: какие сервисы умеют эту ссылку (без учёта токенов). */
    fun optionsForUri(uri: String): List<String> {
        val u = uri.lowercase()
        if (u.startsWith("magnet:")) return listOf(TORRENT)
        if (u.substringBefore("?").endsWith(".torrent")) return listOf(TORRENT)
        if (!u.startsWith("http")) return emptyList()
        return when {
            "gofile.io" in u -> listOf(GOFILE, DIRECT)
            "pixeldrain.com" in u -> listOf(PIXELDRAIN, DIRECT)
            "datanodes.to" in u -> listOf(DATANODES, DIRECT)
            "mediafire.com" in u -> listOf(MEDIAFIRE, DIRECT)
            "fuckingfast.co" in u -> listOf(FUCKINGFAST, DIRECT)
            "vikingfile.com" in u || "vik1ngfile.site" in u -> listOf(VIKINGFILE, DIRECT)
            "rootz.so" in u -> listOf(ROOTZ, DIRECT)
            isArchiveOrgFile(u) -> listOf(ARCHIVEORG, DIRECT)
            else -> listOf(DIRECT)
        }
    }

    suspend fun resolve(uri: String, hoster: String): Debrid.DirectLink? = withContext(Dispatchers.IO) {
        try {
            when (hoster) {
                GOFILE -> gofile(uri)
                PIXELDRAIN -> pixeldrain(uri)
                DATANODES -> hydraUnlock("datanodes", uri)
                VIKINGFILE -> hydraUnlock("vikingfile", uri)
                MEDIAFIRE -> mediafire(uri)
                FUCKINGFAST -> fuckingfast(uri)
                ROOTZ -> rootz(uri)
                ARCHIVEORG -> archiveOrg(uri)
                DIRECT -> Debrid.DirectLink(uri, null, 0)
                else -> null
            }
        } catch (_: Exception) { null }
    }

    // ── HTTP helpers ──

    private fun getText(url: String, headers: Map<String, String> = emptyMap()): String? {
        val req = Request.Builder().url(url).header("User-Agent", BROWSER_UA).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        client.newCall(req).execute().use {
            if (!it.isSuccessful) return null
            return it.body?.string()
        }
    }

    private fun headOk(url: String, ua: String = BROWSER_UA): Boolean {
        return try {
            val req = Request.Builder().url(url).head().header("User-Agent", ua).build()
            client.newCall(req).execute().use { it.code in 200..399 }
        } catch (_: Exception) { false }
    }

    // ── PixelDrain: HEAD cdn, иначе api/file ──

    private fun pixeldrainId(uri: String): String? {
        return try {
            val parts = java.net.URI(uri).path.trim('/').split('/')
            if (parts.size >= 2 && parts[0] == "u") parts[1].ifBlank { null } else null
        } catch (_: Exception) { null }
    }

    private fun pixeldrain(uri: String): Debrid.DirectLink? {
        val id = pixeldrainId(uri) ?: return null
        val cdn = "https://cdn.pixeldrain.eu.cc/$id"
        if (headOk(cdn)) return Debrid.DirectLink(cdn, null, 0)
        if (headOk("https://pixeldrain.com/u/$id")) {
            return Debrid.DirectLink("https://pixeldrain.com/api/file/$id?download", null, 0)
        }
        return null
    }

    // ── Archive.org: чистый парсинг без сети ──

    private fun isArchiveOrgFile(u: String): Boolean {
        return archiveOrgParts(u) != null
    }

    private fun archiveOrgParts(uri: String): Pair<String, String>? {
        return try {
            val url = java.net.URI(uri)
            val host = url.host.lowercase()
            val segs = url.path.trim('/').split('/').filter { it.isNotEmpty() }
            if (".." in segs) return null
            val last = segs.lastOrNull() ?: return null
            if (!Regex("""\.[A-Za-z0-9]{1,10}$""").containsMatchIn(last)) return null
            if (host == "archive.org" && segs.size >= 3 && segs[0] == "download") {
                segs[1] to segs.drop(2).joinToString("/")
            } else if (host.endsWith(".archive.org")) {
                val items = segs.indexOf("items")
                if (items < 0 || items + 2 > segs.size - 1) return null
                segs[items + 1] to segs.drop(items + 2).joinToString("/")
            } else null
        } catch (_: Exception) { null }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun archiveOrg(uri: String): Debrid.DirectLink? {
        val (id, path) = archiveOrgParts(uri) ?: return null
        val encPath = path.split('/').joinToString("/") { enc(it) }
        val url = "https://archive.org/download/${enc(id)}/$encPath"
        return Debrid.DirectLink(url, path.substringAfterLast('/'), 0)
    }

    // ── Mediafire: страница → regex ──

    private fun mediafire(uri: String): Debrid.DirectLink? {
        var u = uri.trim()
        if (Regex("^[a-zA-Z0-9]+$").matches(u)) u = "https://mediafire.com/?$u"
        u = u.replaceFirst("http://", "https://")
        val html = getText(u) ?: return null
        val preDl = Regex("""["']((?:https?:)?//(?:www\.)?mediafire\.com/(?:file|view|download)/[^"']+?dkey=[^"']+)["']""")
            .find(html)?.groupValues?.getOrNull(1)
        if (!preDl.isNullOrBlank()) {
            val full = if (preDl.startsWith("//")) "https:$preDl" else preDl
            // pre-DL страница отдаёт финальную ссылку динамически — пробуем как есть
            // и вторым проходом ищем dynamic-ссылку уже на ней.
            val inner = getText(full)
            val dyn = inner?.let {
                Regex("""https://download\d+\.mediafire\.com/[^"'\\\s]+""").find(it)?.value
            }
            if (!dyn.isNullOrBlank()) return Debrid.DirectLink(dyn, null, 0)
            return Debrid.DirectLink(full, null, 0)
        }
        val dyn = Regex("""https://download\d+\.mediafire\.com/[^"'\\\s]+""").find(html)?.value
        if (!dyn.isNullOrBlank()) return Debrid.DirectLink(dyn, null, 0)
        return null
    }

    // ── FuckingFast: страница → window.open ──

    private fun fuckingfast(uri: String): Debrid.DirectLink? {
        val req = Request.Builder().url(uri).header("User-Agent", FF_UA).build()
        val html = try {
            client.newCall(req).execute().use {
                if (!it.isSuccessful) return null
                it.body?.string() ?: return null
            }
        } catch (_: Exception) { return null }
        val low = html.lowercase()
        if ("rate limit" in low || "file not found or deleted" in low) return null
        val direct = Regex("""window\.open\("(https://fuckingfast\.co/dl/[^"]+)"\)""")
            .find(html)?.groupValues?.getOrNull(1)
        if (direct.isNullOrBlank()) return null
        val name = direct.substringAfter("#", "").takeIf { it.isNotBlank() && !it.startsWith("http") }
        return Debrid.DirectLink(direct, name, 0)
    }

    // ── Rootz: pageToken + api ──

    private fun rootz(uri: String): Debrid.DirectLink? {
        val id = try {
            java.net.URI(uri).path.trim('/').split('/').lastOrNull()?.ifBlank { null }
        } catch (_: Exception) { null } ?: return null
        val pageUrl = "https://www.rootz.so/d/$id"
        val html = getText(
            pageUrl,
            mapOf("Accept" to "text/html", "Referer" to "https://www.rootz.so/")
        ) ?: return null
        val token = Regex("""\\?"pageToken\\?"\s*:\s*\\?"([^"\\]+)""").find(html)
            ?.groupValues?.getOrNull(1)?.ifBlank { null } ?: return null
        val req = Request.Builder()
            .url("https://www.rootz.so/api/files/download-by-short?shortId=${enc(id)}")
            .header("User-Agent", BROWSER_UA)
            .header("Accept", "application/json")
            .header("Referer", pageUrl)
            .header("X-Page-Token", token)
            .build()
        val body = try {
            client.newCall(req).execute().use {
                if (!it.isSuccessful) return null
                it.body?.string() ?: return null
            }
        } catch (_: Exception) { return null }
        return try {
            val j = JSONObject(body)
            if (!j.optBoolean("success", false)) return null
            val data = j.optJSONObject("data") ?: return null
            val fileId = data.optString("fileId").ifBlank { return null }
            if (data.optString("status") != "active") return null
            if (data.optBoolean("passwordProtected", false)) return null
            if (!data.optBoolean("downloadAllowed", true)) return null
            Debrid.DirectLink("https://www.rootz.so/api/files/proxy-download/$fileId", data.optString("fileName").ifBlank { null }, 0)
        } catch (_: Exception) { null }
    }

    // ── Datanodes / VikingFile: анлок через бэкенд Hydra ──

    private suspend fun hydraUnlock(kind: String, uri: String): Debrid.DirectLink? {
        return try {
            val token = HydraApiClient.accessToken
            if (token.isNullOrBlank()) return null
            val raw = if (kind == DATANODES) {
                HydraApiClient.service.unlockDatanodes(mapOf("url" to uri)).string()
            } else {
                HydraApiClient.service.unlockVikingfile(mapOf("url" to uri)).string()
            }
            val link = JSONObject(raw).optString("link").ifBlank { return null }
            Debrid.DirectLink(link, null, 0)
        } catch (_: Exception) { null }
    }

    // ── Gofile: guest-аккаунт best-effort (без wt-токена может не дать) ──

    private fun gofileId(uri: String): Pair<String, String?> {
        val noQuery = uri.substringBefore("?")
        val id = noQuery.trimEnd('/').substringAfterLast('/')
        val pw = Regex("[?&]password=([^&]+)").find(uri)?.groupValues?.getOrNull(1)
        return id to pw
    }

    private fun gofile(uri: String): Debrid.DirectLink? {
        val (id, pw) = gofileId(uri)
        if (id.isBlank()) return null
        // Guest-токен (может не выдаться без website-token — тогда null, честно).
        val token = try {
            val req = Request.Builder()
                .url("https://api.gofile.io/accounts")
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .header("User-Agent", BROWSER_UA)
                .header("Origin", "https://gofile.io")
                .header("Referer", "https://gofile.io/")
                .build()
            client.newCall(req).execute().use {
                if (!it.isSuccessful) return null
                JSONObject(it.body?.string() ?: return null)
                    .optJSONObject("data")?.optString("token")?.ifBlank { null }
            }
        } catch (_: Exception) { return null } ?: return null
        var page = 1
        val seen = HashSet<String>()
        while (page <= 10) {
            val url = StringBuilder("https://api.gofile.io/contents/$id?page=$page&pageSize=1000")
            if (!pw.isNullOrBlank()) url.append("&password=").append(enc(pw))
            val req = Request.Builder()
                .url(url.toString())
                .header("User-Agent", BROWSER_UA)
                .header("Authorization", "Bearer $token")
                .header("Origin", "https://gofile.io")
                .header("Referer", "https://gofile.io/")
                .build()
            val entry: JSONObject = try {
                client.newCall(req).execute().use {
                    if (!it.isSuccessful) return null
                    JSONObject(it.body?.string() ?: return null).optJSONObject("data") ?: return null
                }
            } catch (_: Exception) { return null }
            val children = entry.optJSONObject("children") ?: return null
            val keys = children.keys()
            var totalPages = 1
            while (keys.hasNext()) {
                val k = keys.next()
                if (k == "_meta") continue
                val c = children.optJSONObject(k) ?: continue
                totalPages = maxOf(totalPages, c.optInt("totalPages", 1))
                if (!c.optBoolean("canAccess", true)) continue
                if (c.optString("type") == "file" && !seen.add(c.optString("id"))) continue
                if (c.optString("type") == "file") {
                    val link = c.optString("link")
                    if (link.isBlank()) continue
                    return Debrid.DirectLink(link, c.optString("name").ifBlank { null }, c.optLong("size", 0))
                }
            }
            if (page >= totalPages) break
            page++
        }
        return null
    }
}
