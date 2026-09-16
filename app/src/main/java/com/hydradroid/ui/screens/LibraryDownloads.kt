package com.hydradroid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hydradroid.HydraDroidApp
import com.hydradroid.data.LibraryRepository
import com.hydradroid.data.archive.ArchiveExtractor
import com.hydradroid.data.local.CollectionEntity
import com.hydradroid.data.local.DownloadEntity
import com.hydradroid.data.local.DownloadFolder
import com.hydradroid.data.local.LibraryGameEntity
import com.hydradroid.data.model.GameRepack
import com.hydradroid.data.remote.HydraApiClient
import com.hydradroid.service.DownloadService
import com.hydradroid.ui.components.HydraButton
import com.hydradroid.ui.components.HydraCard
import com.hydradroid.ui.components.HydraEmptyState
import com.hydradroid.ui.components.LibraryCard
import com.hydradroid.ui.components.rememberHydraGridCells
import com.hydradroid.ui.theme.HydraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val LibrarySortLabels = mapOf(
    "recently_played" to "Недавние",
    "most_played" to "По времени",
    "title_asc" to "А–Я",
    "title_desc" to "Я–А"
)

// Порт pages/library: CategoryFilter + коллекции + избранное + sort + view + живой поиск + скан папки.
@Composable
fun LibraryScreen(onGame: (String, String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf("all") } // all|pc|classics
    var collection by remember { mutableStateOf<String?>(null) } // фильтр-коллекция
    var view by remember { mutableStateOf("grid") }
    var sort by remember { mutableStateOf("recently_played") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    // Дебаунс живого поиска: не дёргаем Room на каждую букву.
    var filterDeb by remember { mutableStateOf("") }
    LaunchedEffect(filter) { kotlinx.coroutines.delay(300); filterDeb = filter }
    var games by remember { mutableStateOf<List<LibraryGameEntity>>(emptyList()) }
    var collections by remember { mutableStateOf<List<CollectionEntity>>(emptyList()) }
    var collectionCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var refresh by remember { mutableStateOf(0) }
    var assignGame by remember { mutableStateOf<LibraryGameEntity?>(null) }
    var showCreateCollection by remember { mutableStateOf(false) }
    var showScan by remember { mutableStateOf(false) }
    // Прямое скачивание из библиотеки (как repacks-modal оригинала):
    // кнопка → варианты → диалог запуска → движок.
    var repacksFor by remember { mutableStateOf<LibraryGameEntity?>(null) }
    var repacksList by remember { mutableStateOf<List<com.hydradroid.data.model.GameRepack>>(emptyList()) }
    var repacksLoading by remember { mutableStateOf(false) }
    var libDownload by remember { mutableStateOf<Pair<LibraryGameEntity, com.hydradroid.data.model.GameRepack>?>(null) }
    var libEntity by remember { mutableStateOf<DownloadEntity?>(null) }
    var folderLabel by remember { mutableStateOf("") }
    val pickFolder = rememberFolderPicker { label -> folderLabel = label }

    LaunchedEffect(Unit) {
        folderLabel = DownloadFolder.savedLabel(ctx) ?: DownloadFolder.defaultDir(ctx).absolutePath
    }

    LaunchedEffect(collection, refresh) {
        collections = try { repo.getCollections() } catch (_: Exception) { emptyList() }
        collectionCounts = try { repo.getCollectionCounts() } catch (_: Exception) { emptyMap() }
    }

    LaunchedEffect(category, sort, favoritesOnly, filterDeb, collection, refresh) {
        games = try {
            val base = repo.getGames(sort, category, filterDeb, favoritesOnly)
            if (collection != null) {
                val keys = repo.getCollectionGames(collection!!).map { it.key }.toSet()
                base.filter { it.key in keys }
            } else base
        } catch (_: Exception) { emptyList() }
    }

    fun reload() { refresh++ }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Официальные категории библиотеки из ru-локали: Все / ПК / Классика.
            listOf("all" to "Все", "pc" to "ПК", "classics" to "Классика").forEach { (k, l) ->
                FilterChip(k == category, { category = k; collection = null }, { Text(l) })
            }
            FilterChip(
                favoritesOnly, { favoritesOnly = !favoritesOnly },
                { Text("Избранное") },
                leadingIcon = {
                    Icon(
                        if (favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null, modifier = Modifier.size(16.dp)
                    )
                }
            )
            Spacer(Modifier.weight(1f))
            var sortExp by remember { mutableStateOf(false) }
            var menuExp by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { sortExp = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Сортировка", tint = HydraColors.Muted) }
                DropdownMenu(sortExp, { sortExp = false }) {
                    LibrarySortLabels.forEach { (k, l) ->
                        DropdownMenuItem(text = { Text(l) }, onClick = { sort = k; sortExp = false })
                    }
                }
            }
            Box {
                IconButton(onClick = { menuExp = true }) { Icon(Icons.Default.MoreVert, "Ещё", tint = HydraColors.Muted) }
                DropdownMenu(menuExp, { menuExp = false }) {
                    DropdownMenuItem(
                        text = { Text(if (view == "grid") "Вид: список" else "Вид: сетка") },
                        onClick = { view = if (view == "grid") "large" else "grid"; menuExp = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Новая коллекция") },
                        onClick = { showCreateCollection = true; menuExp = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Сканировать папку") },
                        onClick = { showScan = true; menuExp = false }
                    )
                }
            }
        }
        // Коллекции: фильтр-чипы с количеством (порт collections-filter).
        if (collections.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(collection == null, { collection = null }, { Text("Все коллекции") })
                collections.forEach { c ->
                    FilterChip(
                        collection == c.id, { collection = if (collection == c.id) null else c.id },
                        { Text("${c.name} · ${collectionCounts[c.id] ?: 0}") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close, null, modifier = Modifier.size(16.dp).clickable {
                                    scope.launch { repo.deleteCollection(c.id); if (collection == c.id) collection = null; reload() }
                                }
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        OutlinedTextField(
            value = filter, onValueChange = { filter = it },
            label = { Text("Поиск в библиотеке") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = HydraColors.SecondaryText50) },
            trailingIcon = if (filter.isNotEmpty()) {
                { IconButton(onClick = { filter = "" }) { Icon(Icons.Default.Close, "Очистить", tint = HydraColors.SecondaryText50) } }
            } else null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            singleLine = true
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${LibrarySortLabels[sort]} · ${games.size}",
            color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (games.isEmpty()) {
            HydraEmptyState(
                title = "Ваша библиотека пуста",
                hint = "Добавьте игры из каталога или скачайте их, чтобы начать",
                icon = { Icon(Icons.Default.Book, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(48.dp)) },
                action = { HydraButton("Сканировать папку", { showScan = true }, kind = "outline") }
            )
        } else {
            LazyVerticalGrid(
                if (view == "large") GridCells.Fixed(1) else rememberHydraGridCells(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(games, key = { it.key }, contentType = { "game" }) { g ->
                    LibraryCard(
                        title = g.title, coverUrl = g.coverUrl, favorite = g.favorite,
                        onClick = {
                            if (g.shop == "custom") {
                                val f = g.localPath?.let { File(it) }
                                if (f != null && f.exists()) openFileExternal(ctx, f)
                                else android.widget.Toast.makeText(ctx, "Файл не найден", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (g.shop != "custom") onGame(g.shop, g.objectId)
                        },
                        onFavorite = { scope.launch { repo.toggleFavorite(g.key); reload() } },
                        onRemove = { scope.launch { repo.remove(g.key); reload() } },
                        onCollections = { assignGame = g },
                        onDownload = {
                            if (g.shop == "custom") {
                                val f = g.localPath?.let { File(it) }
                                if (f != null && f.exists()) openFileExternal(ctx, f)
                                else android.widget.Toast.makeText(ctx, "Файл не найден", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                repacksFor = g
                                repacksList = emptyList()
                                repacksLoading = true
                                scope.launch {
                                    try {
                                        val ids = repo.enabledSourceIds()
                                        repacksList = HydraApiClient.service
                                            .getDownloadSources(g.shop, g.objectId, 100, 0, ids)
                                    } catch (_: Exception) {
                                        repacksList = emptyList()
                                    }
                                    repacksLoading = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    assignGame?.let { g ->
        CollectionAssignDialog(
            gameTitle = g.title,
            collections = collections,
            onCreate = { name ->
                scope.launch {
                    val id = repo.createCollection(name)
                    repo.toggleInCollection(g.key, id)
                    reload()
                }
            },
            onToggle = { id -> scope.launch { repo.toggleInCollection(g.key, id); reload() } },
            onDismiss = { assignGame = null; reload() },
            repo = repo, gameKey = g.key
        )
    }
    if (showCreateCollection) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateCollection = false },
            title = { Text("Новая коллекция") },
            text = {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                HydraButton("Создать", {
                    scope.launch { repo.createCollection(name.ifBlank { "Без названия" }); reload() }
                    showCreateCollection = false
                }, kind = "primary", enabled = name.isNotBlank())
            },
            dismissButton = { TextButton({ showCreateCollection = false }) { Text("Отмена") } }
        )
    }
    if (showScan) {
        FolderScanDialog(onDismiss = { showScan = false }, onAdded = { reload() })
    }
    // Диалог вариантов (порт repacks-modal): список → выбор → диалог запуска.
    repacksFor?.let { g ->
        RepacksSheet(
            title = g.title,
            repacks = repacksList,
            loading = repacksLoading,
            onDismiss = { repacksFor = null },
            onPick = { r ->
                repacksFor = null
                libDownload = g to r
                libEntity = DownloadEntity(
                    id = r.id, objectId = g.objectId, shop = g.shop, title = r.title,
                    fileSize = r.fileSize, sourceName = r.downloadSourceName,
                    uri = r.uris.firstOrNull() ?: ""
                )
            }
        )
    }
    val le = libEntity
    val ld = libDownload
    if (le != null && ld != null) {
        DownloadDialog(
            entity = le, repack = ld.second, folderLabel = folderLabel,
            onPickFolder = pickFolder,
            onDismiss = { libEntity = null; libDownload = null },
            onStarted = {
                libEntity = null; libDownload = null
                android.widget.Toast.makeText(ctx, "Загрузка запущена", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/** Диалог назначения коллекций игре (порт create-collection-modal + toggle). */
@Composable
private fun CollectionAssignDialog(
    gameTitle: String,
    collections: List<CollectionEntity>,
    onCreate: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    repo: LibraryRepository,
    gameKey: String
) {
    val scope = rememberCoroutineScope()
    var checked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newName by remember { mutableStateOf("") }
    LaunchedEffect(gameKey) {
        checked = try { repo.getGameCollections(gameKey).toSet() } catch (_: Exception) { emptySet() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Коллекции", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(gameTitle, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
                if (collections.isEmpty()) Text("Коллекций пока нет — создайте первую ниже")
                collections.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            onToggle(c.id)
                            checked = if (c.id in checked) checked - c.id else checked + c.id
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = c.id in checked, onCheckedChange = {
                            onToggle(c.id)
                            checked = if (it) checked + c.id else checked - c.id
                        })
                        Spacer(Modifier.width(8.dp))
                        Text(c.name, modifier = Modifier.weight(1f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        newName, { newName = it.take(60) }, label = { Text("Новая…") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        onCreate(newName.ifBlank { "Без названия" })
                        scope.launch { checked = repo.getGameCollections(gameKey).toSet() }
                        newName = ""
                    }) { Icon(Icons.Default.Add, "Создать", tint = HydraColors.Muted) }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Готово") } }
    )
}

/** Скан папки: выбор SAF-дерева → поиск iso/архивов → добавление в библиотеку. */
@Composable
private fun FolderScanDialog(onDismiss: () -> Unit, onAdded: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<LibraryRepository.ScannedFile>?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var added by remember { mutableStateOf(0) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanning = true
        scope.launch {
            val dir = DownloadFolder.treeUriToRealPath(uri)
            results = if (dir != null && dir.isDirectory) repo.scanFolder(dir) else emptyList()
            scanning = false
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сканирование папки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ищем ISO и архивы (zip/7z/rar) и добавляем их в библиотеку как локальные игры.",
                    color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                )
                HydraButton(if (scanning) "Сканируем…" else "Выбрать папку", { launcher.launch(null) }, kind = "outline", enabled = !scanning, modifier = Modifier.fillMaxWidth())
                results?.let { list ->
                    Text("Найдено: ${list.size}", style = MaterialTheme.typography.bodyMedium)
                    LazyColumn(Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(list.take(100), key = { it.path }) { f ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${f.path.substringAfterLast("/")} · ${DownloadFolder.formatBytes(f.size)}",
                                        color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = {
                                    scope.launch { repo.addCustom(f.name, f.path); added++; onAdded() }
                                }) { Text("Добавить") }
                            }
                        }
                    }
                    if (added > 0) Text("Добавлено: $added", color = HydraColors.Success, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Закрыть") } }
    )
}

/** Диалог вариантов загрузки (порт repacks-modal): фильтр по названию + список репаков. */
@Composable
fun RepacksSheet(
    title: String,
    repacks: List<com.hydradroid.data.model.GameRepack>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onPick: (com.hydradroid.data.model.GameRepack) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val shown = remember(repacks, filter) {
        if (filter.isBlank()) repacks
        else repacks.filter { it.title.contains(filter, ignoreCase = true) }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = HydraColors.DarkBackground) {
            Column(Modifier.padding(16.dp).heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Варианты загрузки", style = MaterialTheme.typography.headlineSmall)
                Text(title, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                OutlinedTextField(
                    filter, { filter = it }, label = { Text("Поиск репаков") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = HydraColors.SecondaryText50) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                when {
                    loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = HydraColors.SecondaryText60)
                    }
                    shown.isEmpty() -> HydraEmptyState(
                        title = "Нет источников",
                        hint = "Проверьте включённые источники в настройках"
                    )
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shown, key = { it.id }) { r ->
                            HydraCard(onClick = { onPick(r) }) {
                                Text(r.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    com.hydradroid.ui.components.HydraBadge(r.downloadSourceName)
                                    Text(
                                        listOfNotNull(r.fileSize, r.uploadDate?.take(10)).joinToString(" · "),
                                        color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Загрузки: порт pages/downloads (download-group) ──
// Группы: активные / ожидают / завершённые / ошибки. Карточка показывает
// прогресс, скорости, ETA, пиры/сиды, раздачу; действия: пауза/продолжить/
// удалить (+файлы), файлы торрента, распаковать, открыть папку.

@Composable
fun DownloadsScreen() {
    val ctx = LocalContext.current
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    val scope = rememberCoroutineScope()
    // Очередь — реактивно из базы: сервис пишет, UI сам обновляется. Никаких таймеров.
    val queue by repo.queueFlow().collectAsState(initial = emptyList())
    var folderLabel by remember { mutableStateOf("") }
    var startDialog by remember { mutableStateOf<DownloadEntity?>(null) }
    var startRepack by remember { mutableStateOf<GameRepack?>(null) }
    var filesDialog by remember { mutableStateOf<String?>(null) } // infoHash
    var localDir by remember { mutableStateOf<File?>(null) }
    var deleteAsk by remember { mutableStateOf<DownloadEntity?>(null) }
    var extractTarget by remember { mutableStateOf<Pair<String, File>?>(null) } // id + archive
    val extractProgress by DownloadService.extractProgress.collectAsState()

    val pickFolder = rememberFolderPicker { label -> folderLabel = label }

    LaunchedEffect(Unit) {
        folderLabel = DownloadFolder.savedLabel(ctx) ?: ""
        if (folderLabel.isBlank()) folderLabel = DownloadFolder.defaultDir(ctx).absolutePath
    }

    val active = queue.filter { it.status == "downloading" || it.status == "fetching" || it.status == "seeding" || it.status == "paused" }
    val waiting = queue.filter { it.status == "queued" || it.kind == "INFO" }
    val done = queue.filter { it.status == "complete" }
    val failed = queue.filter { it.status == "error" }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Загрузки", style = MaterialTheme.typography.headlineSmall)
                Text("${queue.size}", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Движок: libtorrent (DHT, magnet, .torrent, раздача) + HTTP с докачкой и Debrid.",
                color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Папка: ", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
                Text(
                    folderLabel, style = MaterialTheme.typography.bodySmall, color = HydraColors.Body,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                TextButton(onClick = pickFolder) { Text("Изменить") }
            }
        }
        if (queue.isEmpty()) {
            item {
                HydraEmptyState(
                    title = "Здесь так пусто...",
                    hint = "Вы ещё ничего не скачали через Hydra, но никогда не поздно начать.",
                    icon = { Icon(Icons.Default.Download, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(48.dp)) }
                )
            }
        }
        if (active.isNotEmpty()) {
            item { SectionHeader("В процессе (${active.size})") }
            items(active, key = { it.id }) { d ->
                TransferCard(
                    d = d,
                    onPause = { DownloadService.cmd(ctx, DownloadService.ACTION_PAUSE, d.id) },
                    onResume = { DownloadService.cmd(ctx, DownloadService.ACTION_RESUME, d.id) },
                    onCancel = { deleteAsk = d },
                    onFiles = { d.infoHash?.let { filesDialog = it } },
                    onOpenDir = { d.saveDir?.let { localDir = File(it) } },
                    onExtractPoll = { extractProgress[d.id] }
                )
            }
        }
        if (waiting.isNotEmpty()) {
            item { SectionHeader("Загрузки в очереди (${waiting.size})") }
            items(waiting, key = { it.id }) { d ->
                HydraCard {
                    Text(d.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${d.sourceName} · ${d.fileSize ?: "?"}",
                        color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HydraButton("Скачать", {
                            scope.launch {
                                val ids = try { repo.enabledSourceIds() } catch (_: Exception) { emptyList() }
                                val repack = try {
                                    HydraApiClient.service.getDownloadSources(d.shop, d.objectId, 100, 0, ids)
                                        .find { it.id == d.id }
                                } catch (_: Exception) { null }
                                startRepack = repack
                                startDialog = d
                            }
                        }, kind = "primary", modifier = Modifier.weight(1f))
                        HydraButton("Удалить", {
                            scope.launch { repo.dequeue(d.id) }
                        }, kind = "danger", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        if (failed.isNotEmpty()) {
            item { SectionHeader("Ошибки (${failed.size})") }
            items(failed, key = { it.id }) { d ->
                HydraCard {
                    Text(d.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(d.error ?: "Неизвестная ошибка", color = HydraColors.Error, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HydraButton("Повторить", {
                            DownloadService.cmd(ctx, DownloadService.ACTION_START, d.id)
                        }, kind = "primary", modifier = Modifier.weight(1f))
                        HydraButton("Удалить", {
                            scope.launch { repo.dequeue(d.id) }
                        }, kind = "danger", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        if (done.isNotEmpty()) {
            item { SectionHeader("Завершено (${done.size})") }
            items(done, key = { it.id }) { d ->
                // Поиск архива — IO: не блокируем главный поток при прокрутке.
                var arc by remember(d.id, d.saveDir) { mutableStateOf<File?>(null) }
                LaunchedEffect(d.id, d.saveDir) {
                    arc = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        d.saveDir?.let { firstArchiveIn(File(it)) }
                    }
                }
                HydraCard {
                    Text(d.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(
                            d.totalBytes.takeIf { it > 0 }?.let { DownloadFolder.formatBytes(it) },
                            d.saveDir
                        ).joinToString(" · ").ifBlank { "Завершено" },
                        color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                    )
                    val prog = extractProgress[d.id]
                    if (prog != null) {
                        val (ed, et) = prog
                        if (et > 0) LinearProgressIndicator(progress = { (ed.toFloat() / et).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Распаковка… ${if (et > 0) "${(ed * 100 / et).toInt()}%" else ""}", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (d.saveDir != null) {
                            HydraButton("Файлы", { localDir = File(d.saveDir) }, kind = "outline", modifier = Modifier.weight(1f))
                            if (arc != null) HydraButton("Распаковать", {
                                extractTarget = d.id to arc!!
                            }, kind = "primary", modifier = Modifier.weight(1f))
                        }
                        HydraButton("Удалить", {
                            scope.launch { repo.dequeue(d.id) }
                        }, kind = "danger", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    startDialog?.let { d ->
        DownloadDialog(
            entity = d, repack = startRepack, folderLabel = folderLabel,
            onPickFolder = pickFolder,
            onDismiss = { startDialog = null; startRepack = null },
            onStarted = { startDialog = null; startRepack = null }
        )
    }
    filesDialog?.let { hash ->
        TorrentFilesDialog(infoHash = hash, onDismiss = { filesDialog = null })
    }
    localDir?.let { dir ->
        LocalFilesDialog(
            dir = dir,
            onExtract = { f -> localDir = null; extractTarget = ("manual" to f) },
            onDismiss = { localDir = null }
        )
    }
    deleteAsk?.let { d ->
        var withFiles by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { deleteAsk = null },
            title = { Text("Отменить загрузку?") },
            text = {
                Column {
                    Text(d.title, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { withFiles = !withFiles }) {
                        Checkbox(withFiles, { withFiles = it })
                        Text("Удалить файлы с диска")
                    }
                }
            },
            confirmButton = {
                HydraButton("Удалить", {
                    DownloadService.cmd(ctx, DownloadService.ACTION_CANCEL, d.id, withFiles)
                    scope.launch { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        // Сервис тоже чистит запись; страховка:
                        try { repo.dequeue(d.id) } catch (_: Exception) {}
                    }}
                    deleteAsk = null
                }, kind = "danger")
            },
            dismissButton = { TextButton({ deleteAsk = null }) { Text("Отмена") } }
        )
    }
    extractTarget?.let { (id, arc) ->
        AlertDialog(
            onDismissRequest = { extractTarget = null },
            title = { Text("Распаковать?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(arc.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "В папку: ${arc.nameWithoutExtension}",
                        color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                HydraButton("Распаковать", {
                    DownloadService.cmd(ctx, DownloadService.ACTION_EXTRACT, id, archivePath = arc.absolutePath)
                    extractTarget = null
                }, kind = "primary")
            },
            dismissButton = { TextButton({ extractTarget = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
}

private fun firstArchiveIn(dir: File): File? {
    return try {
        dir.walkTopDown().maxDepth(2)
            .filter { it.isFile && ArchiveExtractor.isArchive(it.name) }
            .toList().sortedBy { it.length() }.lastOrNull()
    } catch (_: Exception) { null }
}

/** Карточка активного трансфера: прогресс, скорости, ETA, пиры, раздача, действия. */
@Composable
private fun TransferCard(
    d: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onFiles: () -> Unit,
    onOpenDir: () -> Unit,
    onExtractPoll: () -> Pair<Long, Long>?
) {
    HydraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            StatusChip(d.status)
        }
        if (d.status == "fetching" && d.totalBytes <= 0) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Загрузка метаданных…", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
        } else if (d.kind == "HTTP" && d.totalBytes <= 0 && d.status == "downloading") {
            // Размер неизвестен (chunked): честный indeterminate + счётчик байт.
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                "Загружено ${DownloadFolder.formatBytes(d.doneBytes)}",
                color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
            )
        } else {
            LinearProgressIndicator(
                progress = { d.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${(d.progress * 100).toInt()}% · ${DownloadFolder.formatBytes(d.doneBytes)} из ${DownloadFolder.formatBytes(d.totalBytes)}",
                color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
            )
        }
        val statsLine = buildList {
            if (d.status == "downloading" || d.status == "fetching") {
                add("↓ ${DownloadFolder.formatSpeed(d.downSpeed)}")
                if (d.etaSec >= 0) add("ETA ${DownloadFolder.formatEta(d.etaSec)}")
            }
            if (d.status == "seeding") add("↑ ${DownloadFolder.formatSpeed(d.upSpeed)}")
            if (d.kind == "TORRENT") {
                add("пиры ${d.peers}")
                add("сиды ${d.seeds}")
                if (d.uploadBytes > 0 && d.totalBytes > 0) {
                    add("рейтио %.2f".format(java.util.Locale.US, d.uploadBytes.toDouble() / d.totalBytes))
                }
            }
        }.joinToString(" · ")
        if (statsLine.isNotBlank()) Text(statsLine, color = HydraColors.Body, style = MaterialTheme.typography.bodySmall)
        if (d.error != null && d.status != "error") {
            Text(d.error, color = HydraColors.Warning, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (d.status == "paused") {
                HydraButton("Возобновить", onResume, kind = "primary", modifier = Modifier.weight(1f))
            } else {
                HydraButton("Приостановить", onPause, kind = "outline", modifier = Modifier.weight(1f))
            }
            if (d.kind == "TORRENT" && d.infoHash != null) {
                HydraButton("Файлы", onFiles, kind = "outline", modifier = Modifier.weight(1f))
            } else if (d.saveDir != null) {
                HydraButton("Папка", onOpenDir, kind = "outline", modifier = Modifier.weight(1f))
            }
            HydraButton("Удалить", onCancel, kind = "danger", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "downloading" -> HydraColors.BrandTeal
        "seeding" -> HydraColors.Success
        "paused" -> HydraColors.Warning
        "error" -> HydraColors.Error
        "complete" -> HydraColors.Success
        else -> HydraColors.SecondaryText60
    }
    Box(
        Modifier.padding(start = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(statusLabel(status), color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
