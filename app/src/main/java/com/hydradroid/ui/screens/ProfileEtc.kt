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
import com.hydradroid.data.local.setPref
import com.hydradroid.data.local.stringPrefFlow
import com.hydradroid.data.remote.HydraApiClient
import com.hydradroid.openSignIn
import com.hydradroid.ui.components.HydraButton
import com.hydradroid.ui.components.HydraCard
import com.hydradroid.ui.components.HydraEmptyState
import com.hydradroid.ui.theme.HydraColors
import kotlinx.coroutines.launch

// Порт pages/profile: ProfileHero (баннер + аватар 96 + код друга) + список друзей.
// Вкладки «игры/ачивки» серверного профиля на телефоне-каталоге не нужны:
// библиотека — локальная (экран «Библиотека»), ачивки — на странице игры.
@Composable
fun ProfileScreen(userId: String = "me") {
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
            title = "Вы не вошли",
            hint = "Войдите через Hydra, чтобы видеть профиль и друзей",
            icon = { Icon(Icons.Default.PersonOff, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(48.dp)) },
            action = { HydraButton("Войти", { openSignIn(ctx) }, kind = "cloud") },
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
                    Text(me?.displayName?.ifBlank { null } ?: "Гость", style = MaterialTheme.typography.headlineMedium)
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
                                Toast.makeText(ctx, "Скопировано", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Копировать", tint = HydraColors.SecondaryText60, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (me?.subscription != null) {
                        Text("Hydra Cloud активна", color = HydraColors.Success, style = MaterialTheme.typography.bodySmall)
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
                Text("Друзья", style = MaterialTheme.typography.headlineSmall)
                Text("${friends.size}", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (friends.isEmpty()) {
            item {
                Text(
                    "Список друзей пуст или не загрузился",
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
                            if (f.isOnline) (f.currentGame?.title?.ifBlank { null } ?: "В сети") else "Не в сети",
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
    var loading by remember { mutableStateOf(objectId.isNotBlank()) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(objectId, shop) {
        // Отдельного achievements-эндпоинта в HydraApi нет: данные тянутся
        // со страницы игры. Здесь честно показываем состояние без выдуманных цифр.
        loading = false
        failed = objectId.isBlank()
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Достижения", style = MaterialTheme.typography.headlineMedium)
        HydraCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = HydraColors.Warning, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Очки и разблокировки подтягиваются из Hydra Cloud.",
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
                title = "Нет данных",
                hint = "Откройте достижения со страницы конкретной игры",
                icon = { Icon(Icons.Default.EmojiEvents, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(48.dp)) }
            )
        } else {
            HydraEmptyState(
                title = "Список пуст",
                hint = "У этой игры пока нет синхронизированных достижений",
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
        "FRIEND" in t -> "Друзья"
        "BADGE" in t -> "Награда"
        "REVIEW" in t -> "Отзыв"
        "SOUVENIR" in t -> "Сувенир"
        "RETROACHIEVEMENTS" in t -> "RetroAchievements"
        "CLOUD_GIFT" in t || "CLOUD" in t -> "Hydra Cloud"
        "DOWNLOAD" in t || "EXTRACT" in t -> "Загрузки"
        "ACHIEVEMENT" in t -> "Достижение"
        else -> "Уведомление"
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
    var items by remember { mutableStateOf<List<com.hydradroid.data.model.HydraNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        try { items = HydraApiClient.service.getNotifications().notifications } catch (_: Exception) {}
        loading = false
    }
    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HydraColors.SecondaryText60)
        }
        items.isEmpty() -> HydraEmptyState(
            title = "Нет уведомлений",
            hint = "Здесь появятся новости о достижениях, друзьях и загрузках",
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
                    headlineContent = { Text(notificationTitle(n.type)) },
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
private val AppLanguages = listOf(
    "ru" to "Русский", "en" to "English", "uk" to "Українська", "be" to "Беларуская",
    "pt-BR" to "Português (BR)", "pt-PT" to "Português (PT)", "es" to "Español",
    "de" to "Deutsch", "fr" to "Français", "it" to "Italiano", "pl" to "Polski",
    "tr" to "Türkçe", "zh" to "中文", "ja" to "日本語", "ko" to "한국어",
    "ar" to "العربية", "nl" to "Nederlands", "cs" to "Čeština", "hu" to "Magyar",
    "ro" to "Română", "vi" to "Tiếng Việt", "kk" to "Қазақша", "uz" to "O'zbekcha"
)

@Composable
fun SettingsScreen(onProfile: () -> Unit, onNotifications: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by stringPrefFlow(ctx, PrefKeys.LANGUAGE, "ru").collectAsState(initial = "ru")
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
            Text("Настройки", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            HydraCard {
                Text("Аккаунт Hydra", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                if (signedIn) {
                    Text("Вы вошли", color = HydraColors.Success, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HydraButton("Профиль", onProfile, kind = "outline", modifier = Modifier.weight(1f))
                        HydraButton("Выйти", {
                            TokenStore.clear(ctx)
                            HydraApiClient.accessToken = null
                            signedIn = false
                        }, kind = "danger", modifier = Modifier.weight(1f))
                    }
                } else {
                    Text(
                        "Войдите, чтобы видеть профиль, друзей и облако",
                        color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium
                    )
                    HydraButton("Войти через Hydra", { openSignIn(ctx) }, kind = "cloud", modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            HydraCard {
                Text("Язык", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
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
                            scope.launch { setPref(ctx, PrefKeys.LANGUAGE, code) }; exp = false
                        })
                    }
                }
            }
        }
        item {
            HydraCard {
                Text("Дебрид-сервисы", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Прямые ссылки для загрузок на телефоне",
                    color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(debridEdit, { debridEdit = it }, label = { Text("Real-Debrid API token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(torboxEdit, { torboxEdit = it }, label = { Text("TorBox API token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(premiumizeEdit, { premiumizeEdit = it }, label = { Text("Premiumize API key") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                HydraButton("Сохранить", {
                    scope.launch {
                        setPref(ctx, PrefKeys.REAL_DEBRID_TOKEN, debridEdit)
                        setPref(ctx, PrefKeys.TORBOX_TOKEN, torboxEdit)
                        setPref(ctx, PrefKeys.PREMIUMIZE_TOKEN, premiumizeEdit)
                        savedTick = true
                    }
                }, kind = "primary", modifier = Modifier.fillMaxWidth())
                if (savedTick) Text("Сохранено", color = HydraColors.Success, style = MaterialTheme.typography.bodySmall)
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
                Text("О приложении", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "HydraDroid — каталог, торренты и библиотека Hydra для телефона.",
                    color = HydraColors.Body, style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ── Настройки загрузок (порт settings-context-downloads + global-trackers) ──

@Composable
private fun DownloadsSettingsCard() {
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
        freeSpace = "Свободно на диске: ${DownloadFolder.formatBytes(DownloadFolder.freeBytes(dir))}"
    }
    LaunchedEffect(Unit) { refreshFolder() }
    val pickFolder = rememberFolderPicker { scope.launch { refreshFolder() } }

    HydraCard {
        Text("Загрузки", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Папка", style = MaterialTheme.typography.bodyMedium)
                Text(folderLabel, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(freeSpace, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = pickFolder) { Text("Выбрать") }
        }
        TextButton(onClick = {
            scope.launch { DownloadFolder.clearTree(ctx); refreshFolder() }
        }) { Text("Использовать встроенную папку", color = HydraColors.SecondaryText60) }

        NumberPrefRow("Лимит скачивания (КБ/с, 0 — без лимита)", downKb) {
            scope.launch { setIntPref(ctx, PrefKeys.MAX_DOWNLOAD_SPEED, it) }
        }
        NumberPrefRow("Лимит отдачи (КБ/с, 0 — без лимита)", upKb) {
            scope.launch { setIntPref(ctx, PrefKeys.MAX_UPLOAD_SPEED, it) }
        }
        NumberPrefRow("Макс. соединений (0 — авто)", maxConn) {
            scope.launch { setIntPref(ctx, PrefKeys.MAX_CONNECTIONS, it) }
        }
        NumberPrefRow("Рейтио сидирования ×100 (0 — без лимита)", seedRatio) {
            scope.launch { setIntPref(ctx, PrefKeys.SEED_RATIO, it) }
        }
        NumberPrefRow("Время сидирования (мин, 0 — без лимита)", seedTime) {
            scope.launch { setIntPref(ctx, PrefKeys.SEED_TIME_MIN, it) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Раздавать после завершения", style = MaterialTheme.typography.bodyMedium)
            Switch(seedAfter, { scope.launch { setBoolPref(ctx, PrefKeys.SEED_AFTER_COMPLETE, it) } })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Удалять архив после распаковки", style = MaterialTheme.typography.bodyMedium)
            Switch(deleteArc, { scope.launch { setBoolPref(ctx, PrefKeys.DELETE_ARCHIVE_AFTER_EXTRACT, it) } })
        }
        OutlinedTextField(
            trackersEdit, { trackersEdit = it },
            label = { Text("Дополнительные трекеры (по одному на строку)") },
            modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5
        )
        HydraButton("Сохранить трекеры", {
            scope.launch { setPref(ctx, PrefKeys.GLOBAL_TRACKERS, trackersEdit) }
        }, kind = "outline", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NumberPrefRow(label: String, value: Int, onSave: (Int) -> Unit) {
    var edit by remember(value) { mutableStateOf(if (value == 0) "" else value.toString()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            edit, { edit = it.filter { c -> c.isDigit() }.take(7) },
            label = { Text(label) }, singleLine = true, modifier = Modifier.weight(1f),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { onSave(edit.toIntOrNull() ?: 0) }) { Text("OK") }
    }
}

// ── Источники загрузок (порт settings-download-sources): фильтр каталога ──

@Composable
private fun DownloadSourcesCard() {
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
        Text("Источники загрузок", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Text(
            "Включённые учитываются при поиске в каталоге и дают варианты загрузок. Кнопка ниже добавляет готовый набор популярных источников.",
            color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
        )
        HydraButton(if (busy) "Импорт…" else "Добавить готовые источники", {
            busy = true; note = null
            scope.launch {
                try {
                    val added = com.hydradroid.data.local.BuiltinSources.import(ctx, repo)
                    note = if (added > 0) "Добавлено: $added" else "Всё уже добавлено"
                } catch (e: Exception) {
                    note = "Ошибка: ${e.message?.take(120)}"
                }
                reload()
                busy = false
            }
        }, kind = "primary", enabled = !busy, modifier = Modifier.fillMaxWidth())
        if (note != null) Text(note!!, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
        if (sources.isEmpty()) Text("Источников нет — добавьте первый ниже", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium)
        sources.forEach { s ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(s.enabled, {
                    scope.launch { repo.upsertSource(s.copy(enabled = it)); reload() }
                })
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(s.url, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                IconButton(onClick = { scope.launch { repo.deleteSource(s.id); reload() } }) {
                    Icon(Icons.Default.Delete, "Удалить", tint = HydraColors.SecondaryText60)
                }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(url, { url = it }, label = { Text("URL источника (.json)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        HydraButton(if (busy) "…" else "Добавить по URL", {
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
                        note = "Добавлен: ${resolved.name}"
                    } else {
                        // Сервер отклонил (422): сохраняем локально без fingerprint — позже можно повторить.
                        repo.upsertSource(
                            DownloadSourceEntity(
                                id = "local-${url.trim().hashCode()}",
                                name = name.ifBlank { url.trim() }, url = url.trim()
                            )
                        )
                        note = "Не удалось распознать — сохранено локально"
                    }
                    name = ""; url = ""
                } catch (e: Exception) {
                    note = "Ошибка: ${e.message?.take(120)}"
                }
                reload()
                busy = false
            }
        }, kind = "outline", enabled = url.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth())
    }
}
