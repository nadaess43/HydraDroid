package com.hydradroid.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hydradroid.HydraDroidApp
import com.hydradroid.data.LibraryRepository
import com.hydradroid.data.local.DownloadFolder
import com.hydradroid.data.local.DownloadSourceEntity
import com.hydradroid.data.local.PrefKeys
import com.hydradroid.data.local.TokenStore
import com.hydradroid.data.local.boolPrefFlow
import com.hydradroid.data.local.intPrefFlow
import com.hydradroid.data.local.setBoolPref
import com.hydradroid.data.local.setIntPref
import com.hydradroid.data.local.setLanguage
import com.hydradroid.data.local.setPref
import com.hydradroid.data.local.stringPrefFlow
import com.hydradroid.data.remote.HydraApiClient
import com.hydradroid.ui.components.HydraButton
import com.hydradroid.ui.components.HydraCard
import com.hydradroid.ui.components.HydraEmptyState
import com.hydradroid.ui.i18n.ls
import com.hydradroid.ui.theme.HydraColors
import kotlinx.coroutines.launch

// Порт pages/profile: ProfileHero (баннер + аватар 96 + код друга) + список друзей.
// Вкладки «игры/ачивки» серверного профиля на телефоне-каталоге не нужны:
// библиотека — локальная (экран «Библиотека»), ачивки — на странице игры.
@Composable
fun ProfileScreen(userId: String = "me", onSignIn: () -> Unit = {}) {
    val s = ls()
    var me by remember { mutableStateOf<com.hydradroid.data.model.UserDetails?>(null) }
    var friends by remember { mutableStateOf<List<com.hydradroid.data.model.Friend>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val ctx = LocalContext.current

    LaunchedEffect(userId) {
        try { me = HydraApiClient.service.getMe() } catch (_: Exception) {}
        try { friends = HydraApiClient.service.getFriends().friends } catch (_: Exception) {}
        loading = false
    }

    if (!loading && me == null) {
        // Гость: порт sign-in из SidebarProfile
        HydraEmptyState(
            title = s.t("You are not signed in"),
            hint = s.t("Sign in to see profile, friends and cloud"),
            icon = { Icon(Icons.Default.PersonOff, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(48.dp)) },
            action = { HydraButton(s.t("Sign in"), onSignIn, kind = "cloud") },
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    if (loading && me == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HydraColors.SecondaryText60)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            // ProfileHero: баннер + аватар 96 + имя + username (тап — копировать)
            me?.backgroundImageUrl?.let { AsyncImage(it, null, Modifier.fillMaxWidth().height(140.dp)) }
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (me?.profileImageUrl != null) {
                    AsyncImage(me?.profileImageUrl, null, Modifier.size(96.dp).clip(CircleShape))
                } else {
                    Icon(Icons.Default.AccountCircle, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(96.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(me?.displayName?.ifBlank { null } ?: s.t("Guest"), style = MaterialTheme.typography.headlineMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "@${me?.username?.ifBlank { null } ?: "—"}",
                            color = HydraColors.SecondaryText60,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (!me?.username.isNullOrBlank()) IconButton(
                            onClick = {
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("username", me!!.username))
                                Toast.makeText(ctx, s.t("Copied"), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, s.t("Copy"), tint = HydraColors.SecondaryText60, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (me?.subscription != null) {
                        Text(s.t("Hydra Cloud active"), color = HydraColors.Success, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            HorizontalDivider(color = HydraColors.Border)
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.t("Friends"), style = MaterialTheme.typography.headlineSmall)
                Text("${friends.size}", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (friends.isEmpty()) {
            item {
                Text(
                    s.t("Friend list is empty or failed to load"),
                    color = HydraColors.SecondaryText60,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            items(friends, key = { it.id }) { f ->
                ListItem(
                    headlineContent = { Text(f.displayName.ifBlank { "?" }) },
                    supportingContent = {
                        Text(
                            if (f.isOnline) (f.currentGame?.title?.ifBlank { null } ?: s.t("Online")) else s.t("Offline"),
                            color = if (f.isOnline) HydraColors.Success else HydraColors.SecondaryText60
                        )
                    },
                    leadingContent = {
                        if (f.profileImageUrl != null) {
                            AsyncImage(f.profileImageUrl, null, Modifier.size(48.dp).clip(CircleShape))
                        } else {
                            Icon(Icons.Default.AccountCircle, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(48.dp))
                        }
                    },
                    trailingContent = {
                        if (f.isOnline) Icon(Icons.Default.Circle, null, tint = HydraColors.Success, modifier = Modifier.size(10.dp))
                    }
                )
                HorizontalDivider(color = HydraColors.Border)
            }
        }
    }
}

// Порт pages/achievements: AchievementPanel (очки) + список.
// Разблокировки синхронизирует Hydra Cloud на ПК; здесь — серверные данные игры.
@Composable
fun AchievementsScreen(objectId: String = "", shop: String = "steam") {
    val s = ls()
    var loading by remember { mutableStateOf(objectId.isNotBlank()) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(objectId, shop) {
        // Отдельного achievements-эндпоинта в HydraApi нет: данные тянутся
        // со страницы игры. Здесь честно показываем состояние без выдуманных цифр.
        loading = false
        failed = objectId.isBlank()
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(s.t("Achievements"), style = MaterialTheme.typography.headlineMedium)
        HydraCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = HydraColors.Warning, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    s.t("Points and unlocks are synced from Hydra Cloud."),
                    style = MaterialTheme.typography.bodyMedium, color = HydraColors.Body
                )
            }
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HydraColors.SecondaryText60)
            }
        } else if (failed) {
            HydraEmptyState(
                title = s.t("No data"),
                hint = s.t("Open achievements from a specific game page"),
                icon = { Icon(Icons.Default.EmojiEvents, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(48.dp)) }
            )
        } else {
            HydraEmptyState(
                title = s.t("List is empty"),
                hint = s.t("This game has no synced achievements yet"),
                icon = { Icon(Icons.Default.EmojiEvents, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(48.dp)) }
            )
        }
    }
}

// Реальные типы сервера: FRIEND_REQUEST_RECEIVED/ACCEPTED, BADGE_RECEIVED,
// REVIEW_UPVOTE/ANSWER(_UPVOTE), SOUVENIR_LIKE, RETROACHIEVEMENTS_*, CLOUD_GIFT_RECEIVED.
private fun notificationTitle(type: String): String {
    val t = type.uppercase()
    return when {
        "FRIEND" in t -> "Friends"
        "BADGE" in t -> "Reward"
        "REVIEW" in t -> "Review"
        "SOUVENIR" in t -> "Souvenir"
        "RETROACHIEVEMENTS" in t -> "RetroAchievements"
        "CLOUD_GIFT" in t || "CLOUD" in t -> "Hydra Cloud"
        "DOWNLOAD" in t || "EXTRACT" in t -> "Downloads"
        "ACHIEVEMENT" in t -> "Achievement"
        else -> "Notification"
    }
}

private fun notificationDetail(n: com.hydradroid.data.model.HydraNotification): String? {
    val v = n.variables
    return v["senderDisplayName"] ?: v["accepterDisplayName"] ?: v["displayName"]
        ?: v["badgeName"] ?: v["gameTitle"] ?: v["title"] ?: v["achievementName"]
}

// Порт pages/notifications: иконка по типу + текст + время, непрочитанные — с точкой.
@Composable
fun NotificationsScreen() {
    val s = ls()
    var items by remember { mutableStateOf<List<com.hydradroid.data.model.HydraNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(s.lang) {
        try { items = HydraApiClient.service.getNotifications(if (s.isRu) "ru" else "en").notifications } catch (_: Exception) {}
        loading = false
    }
    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HydraColors.SecondaryText60)
        }
        items.isEmpty() -> HydraEmptyState(
            title = s.t("No notifications"),
            hint = s.t("News about achievements, friends and downloads will appear here"),
            icon = { Icon(Icons.Default.NotificationsOff, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(48.dp)) },
            modifier = Modifier.fillMaxSize()
        )
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { n ->
                val upper = n.type.uppercase()
                val icon = when {
                    "BADGE" in upper || "ACHIEVEMENT" in upper -> Icons.Default.EmojiEvents
                    "FRIEND" in upper -> Icons.Default.PersonAdd
                    "DOWNLOAD" in upper || "EXTRACT" in upper -> Icons.Default.Download
                    "CLOUD" in upper -> Icons.Default.Cloud
                    "REVIEW" in upper -> Icons.Default.RateReview
                    else -> Icons.Default.Notifications
                }
                val detail = notificationDetail(n)
                ListItem(
                    headlineContent = { Text(s.t(notificationTitle(n.type))) },
                    supportingContent = {
                        Column {
                            if (detail != null) Text(detail, color = HydraColors.Body)
                            Text(
                                n.createdAt.take(16).replace("T", " "),
                                color = HydraColors.SecondaryText60
                            )
                        }
                    },
                    leadingContent = { Icon(icon, null, tint = HydraColors.SecondaryText60) },
                    trailingContent = {
                        if (!n.isRead) Icon(Icons.Default.Circle, null, tint = HydraColors.BrandTeal, modifier = Modifier.size(8.dp))
                    }
                )
                HorizontalDivider(color = HydraColors.Border)
            }
        }
    }
}

// Порт pages/settings: секции Account / Language / Debrid / Downloads / Sources / About
// одной колонкой — так удобно на телефоне.
private val AppLanguages = listOf("en" to "English", "ru" to "Русский")

@Composable
fun SettingsScreen(onProfile: () -> Unit, onNotifications: () -> Unit, onSignIn: () -> Unit = {}) {
    val s = ls()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by stringPrefFlow(ctx, PrefKeys.LANGUAGE, "en").collectAsState(initial = "en")
    val debrid by stringPrefFlow(ctx, PrefKeys.REAL_DEBRID_TOKEN).collectAsState(initial = "")
    val torbox by stringPrefFlow(ctx, PrefKeys.TORBOX_TOKEN).collectAsState(initial = "")
    val premiumize by stringPrefFlow(ctx, PrefKeys.PREMIUMIZE_TOKEN, "").collectAsState(initial = "")
    var debridEdit by remember(debrid) { mutableStateOf(debrid) }
    var torboxEdit by remember(torbox) { mutableStateOf(torbox) }
    var premiumizeEdit by remember(premiumize) { mutableStateOf(premiumize) }
    var signedIn by remember { mutableStateOf(TokenStore.isSignedIn(ctx)) }
    var savedTick by remember { mutableStateOf(false) }
    // Возврат из браузера после входа: обновляем состояние (иначе висит «Войти»).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                try { signedIn = TokenStore.isSignedIn(ctx) } catch (_: Exception) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(s.t("Settings"), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            HydraCard {
                Text(s.t("Hydra account"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                if (signedIn) {
                    Text(s.t("Signed in"), color = HydraColors.Success, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HydraButton(s.t("Profile"), onProfile, kind = "outline", modifier = Modifier.weight(1f))
                        HydraButton(s.t("Sign out"), {
                            TokenStore.clear(ctx)
                            HydraApiClient.accessToken = null
                            signedIn = false
                        }, kind = "danger", modifier = Modifier.weight(1f))
                    }
                } else {
                    Text(
                        s.t("Sign in to see profile, friends and cloud"),
                        color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium
                    )
                    HydraButton(s.t("Sign in with Hydra"), onSignIn, kind = "cloud", modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            HydraCard {
                Text(s.t("Language"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                var exp by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { exp = true }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HydraColors.Muted)
                ) {
                    Text(AppLanguages.find { it.first == language }?.second ?: language)
                }
                DropdownMenu(exp, { exp = false }) {
                    AppLanguages.forEach { (code, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            scope.launch { setLanguage(ctx, code) }; exp = false
                        })
                    }
                }
            }
        }
        item {
            HydraCard {
                Text(s.t("Debrid services"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    s.t("Direct links for downloads on your phone"),
                    color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(debridEdit, { debridEdit = it }, label = { Text(s.t("Real-Debrid API token")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(torboxEdit, { torboxEdit = it }, label = { Text(s.t("TorBox API token")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(premiumizeEdit, { premiumizeEdit = it }, label = { Text(s.t("Premiumize API key")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                HydraButton(s.t("Save"), {
                    scope.launch {
                        setPref(ctx, PrefKeys.REAL_DEBRID_TOKEN, debridEdit)
                        setPref(ctx, PrefKeys.TORBOX_TOKEN, torboxEdit)
                        setPref(ctx, PrefKeys.PREMIUMIZE_TOKEN, premiumizeEdit)
                        savedTick = true
                    }
                }, kind = "primary", modifier = Modifier.fillMaxWidth())
                if (savedTick) Text(s.t("Saved"), color = HydraColors.Success, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            DownloadsSettingsCard()
        }
        item {
            DownloadSourcesCard()
        }
        item {
            HydraCard {
                Text(s.t("About"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    s.t("HydraDroid — Hydra catalogue, torrents and library for your phone."),
                    color = HydraColors.Body, style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ── Настройки загрузок (порт settings-context-downloads + global-trackers) ──

@Composable
private fun DownloadsSettingsCard() {
    val s = ls()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val downKb by intPrefFlow(ctx, PrefKeys.MAX_DOWNLOAD_SPEED, 0).collectAsState(initial = 0)
    val upKb by intPrefFlow(ctx, PrefKeys.MAX_UPLOAD_SPEED, 0).collectAsState(initial = 0)
    val maxConn by intPrefFlow(ctx, PrefKeys.MAX_CONNECTIONS, 0).collectAsState(initial = 0)
    val seedRatio by intPrefFlow(ctx, PrefKeys.SEED_RATIO, 0).collectAsState(initial = 0)
    val seedTime by intPrefFlow(ctx, PrefKeys.SEED_TIME_MIN, 0).collectAsState(initial = 0)
    val seedAfter by boolPrefFlow(ctx, PrefKeys.SEED_AFTER_COMPLETE, true).collectAsState(initial = true)
    val trackers by stringPrefFlow(ctx, PrefKeys.GLOBAL_TRACKERS, "").collectAsState(initial = "")
    val deleteArc by boolPrefFlow(ctx, PrefKeys.DELETE_ARCHIVE_AFTER_EXTRACT, false).collectAsState(initial = false)
    var folderLabel by remember { mutableStateOf("") }
    var freeSpace by remember { mutableStateOf("") }
    var trackersEdit by remember(trackers) { mutableStateOf(trackers) }

    suspend fun refreshFolder() {
        val label = DownloadFolder.savedLabel(ctx) ?: DownloadFolder.defaultDir(ctx).absolutePath
        folderLabel = label
        val dir = try { DownloadFolder.resolveRealDir(ctx) } catch (_: Exception) { DownloadFolder.defaultDir(ctx) }
        freeSpace = s.t("Free space: {0}", DownloadFolder.formatBytes(DownloadFolder.freeBytes(dir)))
    }
    LaunchedEffect(Unit) { refreshFolder() }
    val pickFolder = rememberFolderPicker { scope.launch { refreshFolder() } }

    HydraCard {
        Text(s.t("Downloads"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.t("Download folder"), style = MaterialTheme.typography.bodyMedium)
                Text(folderLabel, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(freeSpace, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = pickFolder) { Text(s.t("Choose")) }
        }
        TextButton(onClick = {
            scope.launch { DownloadFolder.clearTree(ctx); refreshFolder() }
        }) { Text(s.t("Use built-in folder"), color = HydraColors.SecondaryText60) }

        NumberPrefRow(s.t("Download limit (KB/s, 0 — unlimited)"), downKb) {
            scope.launch { setIntPref(ctx, PrefKeys.MAX_DOWNLOAD_SPEED, it) }
        }
        NumberPrefRow(s.t("Upload limit (KB/s, 0 — unlimited)"), upKb) {
            scope.launch { setIntPref(ctx, PrefKeys.MAX_UPLOAD_SPEED, it) }
        }
        NumberPrefRow(s.t("Max connections (0 — auto)"), maxConn) {
            scope.launch { setIntPref(ctx, PrefKeys.MAX_CONNECTIONS, it) }
        }
        NumberPrefRow(s.t("Seeding ratio ×100 (0 — unlimited)"), seedRatio) {
            scope.launch { setIntPref(ctx, PrefKeys.SEED_RATIO, it) }
        }
        NumberPrefRow(s.t("Seeding time (min, 0 — unlimited)"), seedTime) {
            scope.launch { setIntPref(ctx, PrefKeys.SEED_TIME_MIN, it) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(s.t("Seed after completion"), style = MaterialTheme.typography.bodyMedium)
            Switch(seedAfter, { scope.launch { setBoolPref(ctx, PrefKeys.SEED_AFTER_COMPLETE, it) } })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(s.t("Delete archive after extraction"), style = MaterialTheme.typography.bodyMedium)
            Switch(deleteArc, { scope.launch { setBoolPref(ctx, PrefKeys.DELETE_ARCHIVE_AFTER_EXTRACT, it) } })
        }
        OutlinedTextField(
            trackersEdit, { trackersEdit = it },
            label = { Text(s.t("Additional trackers (one per line)")) },
            modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5
        )
        HydraButton(s.t("Save trackers"), {
            scope.launch { setPref(ctx, PrefKeys.GLOBAL_TRACKERS, trackersEdit) }
        }, kind = "outline", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NumberPrefRow(label: String, value: Int, onSave: (Int) -> Unit) {
    val s = ls()
    var edit by remember(value) { mutableStateOf(if (value == 0) "" else value.toString()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            edit, { edit = it.filter { c -> c.isDigit() }.take(7) },
            label = { Text(label) }, singleLine = true, modifier = Modifier.weight(1f),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { onSave(edit.toIntOrNull() ?: 0) }) { Text(s.t("OK")) }
    }
}

// ── Источники загрузок (порт settings-download-sources): фильтр каталога ──

@Composable
private fun DownloadSourcesCard() {
    val s = ls()
    val ctx = LocalContext.current
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<DownloadSourceEntity>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        sources = try { repo.getSources() } catch (_: Exception) { emptyList() }
    }
    fun reload() {
        scope.launch { sources = try { repo.getSources() } catch (_: Exception) { emptyList() } }
    }
    HydraCard {
        Text(s.t("Download sources"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Text(
            s.t("Enabled sources are used for catalogue search and provide download options. The button below adds a bundled set of popular sources."),
            color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
        )
        HydraButton(if (busy) s.t("Importing…") else s.t("Add bundled sources"), {
            busy = true; note = null
            scope.launch {
                try {
                    val added = com.hydradroid.data.local.BuiltinSources.import(ctx, repo)
                    note = if (added > 0) s.t("Added: {0}", added) else s.t("Everything already added")
                } catch (e: Exception) {
                    note = s.t("Error: {0}", e.message?.take(120))
                }
                reload()
                busy = false
            }
        }, kind = "primary", enabled = !busy, modifier = Modifier.fillMaxWidth())
        if (note != null) Text(note!!, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
        if (sources.isEmpty()) Text(s.t("No sources — add your first one below"), color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium)
        sources.forEach { src ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(src.enabled, {
                    scope.launch { repo.upsertSource(src.copy(enabled = it)); reload() }
                })
                Column(Modifier.weight(1f)) {
                    Text(src.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(src.url, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                IconButton(onClick = { scope.launch { repo.deleteSource(src.id); reload() } }) {
                    Icon(Icons.Default.Delete, s.t("Delete"), tint = HydraColors.SecondaryText60)
                }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text(s.t("Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(url, { url = it }, label = { Text(s.t("Source URL (.json)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        HydraButton(if (busy) "…" else s.t("Add by URL"), {
            busy = true; note = null
            scope.launch {
                try {
                    // Порт addDownloadSource: сервер возвращает каноническую запись с fingerprint.
                    val resolved = try {
                        HydraApiClient.service.addDownloadSource(mapOf("url" to url.trim()))
                    } catch (e: Exception) { null }
                    if (resolved != null) {
                        repo.upsertSource(
                            DownloadSourceEntity(
                                id = resolved.id, name = resolved.name, url = resolved.url,
                                fingerprint = resolved.fingerprint
                            )
                        )
                        note = s.t("Added: {0}", resolved.name)
                    } else {
                        // Сервер отклонил (422): сохраняем локально без fingerprint — позже можно повторить.
                        repo.upsertSource(
                            DownloadSourceEntity(
                                id = "local-${url.trim().hashCode()}",
                                name = name.ifBlank { url.trim() }, url = url.trim()
                            )
                        )
                        note = s.t("Could not parse — saved locally")
                    }
                    name = ""; url = ""
                } catch (e: Exception) {
                    note = s.t("Error: {0}", e.message?.take(120))
                }
                reload()
                busy = false
            }
        }, kind = "outline", enabled = url.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth())
    }
}
