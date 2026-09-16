package com.hydradroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.hydradroid.HydraDroidApp
import com.hydradroid.data.LibraryRepository
import com.hydradroid.data.model.*
import com.hydradroid.data.remote.HydraApiClient
import com.hydradroid.ui.components.*
import com.hydradroid.ui.theme.HydraColors
import kotlinx.coroutines.launch

// Порт pages/game-details: hero full-bleed + описание (Show more/less) +
// GallerySlider + отзывы + репаки/HLTB/ProtonDB + реальное «В библиотеку».
@Composable
fun GameDetailsScreen(
    shop: String,
    objectId: String,
    onAchievements: (String, String) -> Unit,
    onBack: () -> Unit,
    onRollRandom: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    val scope = rememberCoroutineScope()
    var assets by remember { mutableStateOf<ShopAssets?>(null) }
    var details by remember { mutableStateOf<SteamAppDetails?>(null) }
    var stats by remember { mutableStateOf<GameStats?>(null) }
    var repacks by remember { mutableStateOf<List<GameRepack>>(emptyList()) }
    var hltb by remember { mutableStateOf<List<HowLongToBeatCategory>>(emptyList()) }
    var proton by remember { mutableStateOf<ProtonDBData?>(null) }
    var reviews by remember { mutableStateOf<List<GameReview>>(emptyList()) }
    var added by remember { mutableStateOf(false) }
    var expandedDesc by remember { mutableStateOf(false) }
    var fullscreenShot by remember { mutableStateOf<String?>(null) }
    var queuedMsg by remember { mutableStateOf<String?>(null) }
    var downloadRepack by remember { mutableStateOf<GameRepack?>(null) }
    var downloadEntity by remember { mutableStateOf<com.hydradroid.data.local.DownloadEntity?>(null) }
    var showAllRepacks by remember { mutableStateOf(false) }
    var folderLabel by remember { mutableStateOf("") }
    val pickFolder = rememberFolderPicker { label -> folderLabel = label }

    LaunchedEffect(shop, objectId) {
        folderLabel = com.hydradroid.data.local.DownloadFolder.savedLabel(ctx)
            ?: com.hydradroid.data.local.DownloadFolder.defaultDir(ctx).absolutePath
        // ID источников пользователя — без них сервер отдаёт пустые варианты (как в оригинале).
        val sourceIds = try { repo.enabledSourceIds() } catch (_: Exception) { emptyList() }
        try { assets = HydraApiClient.service.getGameAssets(shop, objectId) } catch (_: Exception) {}
        try {
            details = HydraApiClient.service.getShopDetails(
                mapOf("shop" to shop, "objectIds" to listOf(objectId), "language" to "russian")
            ).firstOrNull()?.data?.data
        } catch (_: Exception) {}
        try { stats = HydraApiClient.service.getGameStats(shop, objectId) } catch (_: Exception) {}
        try { repacks = HydraApiClient.service.getDownloadSources(shop, objectId, 100, 0, sourceIds) } catch (_: Exception) {}
        try { hltb = HydraApiClient.service.getHltb(shop, objectId) } catch (_: Exception) {}
        try { proton = HydraApiClient.service.getProtonDb(shop, objectId) } catch (_: Exception) {}
        try { reviews = HydraApiClient.service.getReviews(shop, objectId).reviews } catch (_: Exception) {}
        try { added = repo.isAdded(shop, objectId) } catch (_: Exception) {}
    }

    val plainDesc = remember(details) {
        com.hydradroid.ui.cleanHtml(
            details?.detailed_description ?: details?.about_the_game ?: details?.short_description
        )
    }
    val title = assets?.title ?: details?.name ?: "Загрузка…"
    val genreNames = remember(details) { details?.genres?.map { it.description } ?: emptyList() }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            // HeroPanel: full-bleed арт + градиент + название + бейджи + действия
            Box(Modifier.fillMaxWidth().height(260.dp).background(HydraColors.SkeletonBase)) {
                assets?.libraryHeroImageUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                Box(Modifier.fillMaxSize().background(HydraColors.HeroBackdrop))
                Column(
                    Modifier.align(Alignment.BottomStart).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        proton?.let { ProtonBadge(it.tier) }
                        genreNames.take(2).forEach { HydraBadge(it) }
                    }
                    stats?.let { GameSpecifics(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Игра уже в библиотеке: вместо мёртвой надписи — кнопка скачивания,
                        // дальше варианты как в оригинале (repacks-modal → движок).
                        if (added) HydraButton(
                            "Скачать", { showAllRepacks = true },
                            kind = "primary", modifier = Modifier.weight(1f)
                        )
                        else HydraButton(
                            "В библиотеку", {
                                scope.launch {
                                    val g = CatalogueSearchResult(
                                        id = objectId, objectId = objectId, title = title,
                                        shop = shop, libraryImageUrl = assets?.libraryImageUrl,
                                        coverImageUrl = assets?.coverImageUrl
                                    )
                                    repo.add(g); added = true
                                }
                            }, kind = "primary", modifier = Modifier.weight(1f)
                        )
                        HydraButton(
                            "Достижения", { onAchievements(shop, objectId) },
                            kind = "outline", modifier = Modifier.weight(1f)
                        )
                        // Кубик «Удиви меня» прямо на странице игры — следующая случайная без возврата.
                        IconButton(onClick = onRollRandom, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Casino, "Удиви меня", tint = HydraColors.Muted, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
        // Описание (collapsed → Показать больше/Скрыть)
        if (plainDesc.isNotBlank()) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Об игре", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        plainDesc,
                        maxLines = if (expandedDesc) Int.MAX_VALUE else 6,
                        color = HydraColors.Body, style = MaterialTheme.typography.bodyLarge
                    )
                    TextButton({ expandedDesc = !expandedDesc }) {
                        Text(
                            if (expandedDesc) "Скрыть" else "Показать больше",
                            color = HydraColors.SecondaryText60
                        )
                    }
                }
            }
        }
        // GallerySlider: screenshots + fullscreen
        val shots = (details?.screenshots?.map { it.path_full }?.filter { it.isNotBlank() } ?: emptyList()).take(10)
        if (shots.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Галерея", style = MaterialTheme.typography.headlineSmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        shots.forEach { url ->
                            AsyncImage(url, null, modifier = Modifier.width(240.dp).height(135.dp)
                                .clip(MaterialTheme.shapes.small).clickable { fullscreenShot = url },
                                contentScale = ContentScale.Crop)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Варианты загрузки", count = repacks.size.takeIf { it > 0 })
                if (repacks.isEmpty()) Text(
                    "Нет источников",
                    color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium
                )
                repacks.take(20).forEach { r ->
                    HydraCard {
                        Text(r.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            HydraBadge(r.downloadSourceName)
                            Text(
                                listOfNotNull(r.fileSize, r.uploadDate?.take(10)).joinToString(" · "),
                                color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (r.uris.isEmpty() && r.unavailableUris.isEmpty()) {
                            Text(
                                "Ссылки уточняются — откройте позже",
                                color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HydraButton("Скачать", {
                                downloadRepack = r
                                downloadEntity = com.hydradroid.data.local.DownloadEntity(
                                    id = r.id, objectId = objectId, shop = shop, title = r.title,
                                    fileSize = r.fileSize, sourceName = r.downloadSourceName,
                                    uri = r.uris.firstOrNull() ?: ""
                                )
                            }, kind = "primary", enabled = r.uris.isNotEmpty() || r.unavailableUris.isNotEmpty())
                            HydraButton("В очередь", {
                                scope.launch { repo.enqueueRepack(shop, objectId, r); queuedMsg = "«${r.title}» добавлен в очередь загрузок" }
                            }, kind = "outline")
                        }
                    }
                }
                if (repacks.size > 20) {
                    HydraButton(
                        "Показать все (${repacks.size})",
                        { showAllRepacks = true },
                        kind = "outline", modifier = Modifier.fillMaxWidth()
                    )
                }
                if (hltb.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Время прохождения", style = MaterialTheme.typography.headlineSmall)
                    hltb.forEach {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(it.title, color = HydraColors.Body, style = MaterialTheme.typography.bodyMedium)
                            Text(it.duration, color = HydraColors.Muted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                SectionTitle("Отзывы", count = reviews.size.takeIf { it > 0 })
                if (reviews.isEmpty()) Text(
                    "Пока нет отзывов",
                    color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium
                )
                reviews.take(10).forEach { rev ->
                    val author = rev.user?.displayName?.ifBlank { null } ?: "Игрок"
                    val text = com.hydradroid.ui.cleanHtml(
                        rev.translations["ru"] ?: rev.translations["en"] ?: rev.reviewHtml
                    ).take(400)
                    HydraCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, null, tint = HydraColors.Warning, modifier = Modifier.size(14.dp))
                                Text("${rev.score}", color = HydraColors.Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (text.isNotBlank()) Text(text, color = HydraColors.Body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    fullscreenShot?.let { url ->
        Dialog(onDismissRequest = { fullscreenShot = null }) {
            AsyncImage(url, null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
        }
    }
    queuedMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { queuedMsg = null },
            title = { Text("Очередь") }, text = { Text(msg) },
            confirmButton = { TextButton({ queuedMsg = null }) { Text("OK") } }
        )
    }
    val de = downloadEntity
    if (de != null) {
        DownloadDialog(
            entity = de, repack = downloadRepack, folderLabel = folderLabel,
            onPickFolder = pickFolder,
            onDismiss = { downloadEntity = null; downloadRepack = null },
            onStarted = {
                downloadEntity = null; downloadRepack = null
                queuedMsg = "Загрузка запущена — прогресс на вкладке «Загрузки»"
            }
        )
    }
    if (showAllRepacks) {
        RepacksSheet(
            title = title,
            repacks = repacks,
            loading = false,
            onDismiss = { showAllRepacks = false },
            onPick = { r ->
                showAllRepacks = false
                downloadRepack = r
                downloadEntity = com.hydradroid.data.local.DownloadEntity(
                    id = r.id, objectId = objectId, shop = shop, title = r.title,
                    fileSize = r.fileSize, sourceName = r.downloadSourceName,
                    uri = r.uris.firstOrNull() ?: ""
                )
            }
        )
    }
}
