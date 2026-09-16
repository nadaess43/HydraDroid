package com.hydradroid.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.hydradroid.HydraDroidApp
import com.hydradroid.data.LibraryRepository
import com.hydradroid.data.archive.ArchiveExtractor
import com.hydradroid.data.hosters.Hosters
import com.hydradroid.data.local.DownloadEntity
import com.hydradroid.data.local.DownloadFolder
import com.hydradroid.data.local.LangStore
import com.hydradroid.data.local.PrefKeys
import com.hydradroid.data.local.stringPrefFlow
import com.hydradroid.data.model.GameRepack
import com.hydradroid.data.torrent.TorrentEngine
import com.hydradroid.service.DownloadService
import com.hydradroid.ui.components.HydraButton
import com.hydradroid.ui.components.HydraEmptyState
import com.hydradroid.ui.i18n.Str
import com.hydradroid.ui.i18n.ls
import com.hydradroid.ui.theme.HydraColors
import kotlinx.coroutines.launch
import java.io.File

// ── Общие трансферные UI: диалог запуска, файлы торрента, локальные файлы ──

fun detectKind(uri: String): String = when {
    uri.startsWith("magnet:") -> "TORRENT"
    uri.lowercase().substringBefore("?").endsWith(".torrent") -> "TORRENT"
    else -> "HTTP"
}

/** Вариант сервиса для ссылки: id + подпись (brand как есть, ключи — через s.t на вызове). */
data class SvcOpt(val id: String, val title: String, val arg: String? = null)

fun debridBrand(id: String): String = when (id) {
    "rd", Hosters.RD_REMOTE -> "Real-Debrid"
    "tb", Hosters.TB_REMOTE -> "TorBox"
    "pm", Hosters.PM_REMOTE -> "Premiumize"
    else -> Hosters.label(id)
}

/** Порт download-settings-modal: доступные даунлоадеры для конкретной ссылки. */
fun serviceOptions(uri: String, rd: String, tb: String, pm: String): List<SvcOpt> {
    val opts = Hosters.optionsForUri(uri)
    if (Hosters.TORRENT in opts) {
        val l = mutableListOf(SvcOpt(Hosters.TORRENT, Hosters.label(Hosters.TORRENT)))
        if (rd.isNotBlank()) l += SvcOpt(Hosters.RD_REMOTE, debridBrand(Hosters.RD_REMOTE))
        if (tb.isNotBlank()) l += SvcOpt(Hosters.TB_REMOTE, debridBrand(Hosters.TB_REMOTE))
        if (pm.isNotBlank()) l += SvcOpt(Hosters.PM_REMOTE, debridBrand(Hosters.PM_REMOTE))
        return l
    }
    val l = opts.map {
        if (it == Hosters.DIRECT) SvcOpt(it, "Direct link") else SvcOpt(it, Hosters.label(it))
    }.toMutableList()
    if (rd.isNotBlank()) l += SvcOpt("rd", "Via {0}", debridBrand("rd"))
    if (tb.isNotBlank()) l += SvcOpt("tb", "Via {0}", debridBrand("tb"))
    if (pm.isNotBlank()) l += SvcOpt("pm", "Via {0}", debridBrand("pm"))
    return l
}

// Официальные статусы из ru-локали (downloads).
@Composable
fun statusLabel(status: String): String {
    val s = ls()
    return when (status) {
        "queued" -> s.t("Queued")
        "fetching" -> s.t("Fetching metadata")
        "downloading" -> s.t("Downloading")
        "paused" -> s.t("Paused")
        "seeding" -> s.t("Seeding")
        "complete" -> s.t("Complete")
        "error" -> s.t("Error")
        else -> status
    }
}

/** Хоcт SAF-пикера с доступом к Context (пикер + сохранение дерева). */
@Composable
fun rememberFolderPicker(onPicked: (label: String) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val label = try {
                DocumentFile.fromTreeUri(ctx, uri)?.name ?: uri.toString()
            } catch (_: Exception) { uri.toString() }
            DownloadFolder.saveTree(ctx, uri, label)
            onPicked(label)
        }
    }
    return { launcher.launch(null) }
}

/** Запрос POST_NOTIFICATIONS на Android 13+ перед стартом загрузок. */
@Composable
fun rememberNotifPermission(): () -> Unit {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    ctx, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    return {
        if (Build.VERSION.SDK_INT >= 33 && !granted) {
            try { launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS) } catch (_: Exception) {}
        }
    }
}

/**
 * Диалог запуска скачивания репака: выбор источника (magnet/.torrent/HTTP-ссылки),
 * типа движка, папки и имени файла. Старт — через DownloadService.
 */
@Composable
fun DownloadDialog(
    entity: DownloadEntity,
    repack: GameRepack?,
    folderLabel: String,
    onPickFolder: () -> Unit,
    onDismiss: () -> Unit,
    onStarted: () -> Unit
) {
    val ctx = LocalContext.current
    val s = ls()
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    val scope = rememberCoroutineScope()
    val askNotif = rememberNotifPermission()

    val uris = remember(repack, entity) {
        val all = (repack?.uris ?: emptyList()) + (repack?.unavailableUris ?: emptyList())
        (if (all.isNotEmpty()) all.distinct() else listOf(entity.uri)).filter { it.isNotBlank() }
    }
    var chosenUri by remember { mutableStateOf(uris.firstOrNull { it.startsWith("magnet:") } ?: uris.firstOrNull() ?: "") }
    val rdTok by stringPrefFlow(ctx, PrefKeys.REAL_DEBRID_TOKEN, "").collectAsState(initial = "")
    val tbTok by stringPrefFlow(ctx, PrefKeys.TORBOX_TOKEN, "").collectAsState(initial = "")
    val pmTok by stringPrefFlow(ctx, PrefKeys.PREMIUMIZE_TOKEN, "").collectAsState(initial = "")
    // Сервис как в оригинале (download-settings-modal): движок выводится из него,
    // руками TORRENT/HTTP больше не переключаем — это и давало битые связки.
    val services = remember(chosenUri, rdTok, tbTok, pmTok) {
        serviceOptions(chosenUri, rdTok, tbTok, pmTok)
    }
    var service by remember(chosenUri, services) {
        mutableStateOf(services.firstOrNull()?.id ?: Hosters.DIRECT)
    }
    val kind = if (service == Hosters.TORRENT) "TORRENT" else "HTTP"
    var fileName by remember { mutableStateOf(entity.fileName?.ifBlank { null } ?: chosenUri.substringAfterLast("/").substringBefore("?").ifBlank { "${entity.title}.bin" }) }
    var starting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!starting) onDismiss() },
        title = { Text(s.t("Download"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entity.title, fontWeight = FontWeight.Bold)
                Text("${entity.sourceName} · ${entity.fileSize ?: "?"}", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
                if (uris.size > 1) {
                    Text(s.t("Source ({0})", uris.size), style = MaterialTheme.typography.bodySmall)
                    var exp by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { exp = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            chosenUri.take(48) + if (chosenUri.length > 48) "…" else "",
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(exp, { exp = false }) {
                        uris.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u.take(60), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { chosenUri = u; exp = false }
                            )
                        }
                    }
                }
                // Сервис загрузки как в оригинале (выбор даунлоадера под ссылку).
                Text(s.t("Service"), style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    services.forEach { opt ->
                        val label = if (opt.arg != null) s.t(opt.title, opt.arg)
                            else if (opt.id == Hosters.DIRECT) s.t("Direct link")
                            else opt.title
                        FilterChip(service == opt.id, { service = opt.id }, { Text(label, maxLines = 1) })
                    }
                }
                val svcHint = when {
                    service == Hosters.TORRENT -> s.t("Downloads via torrents: with resume and seeding.")
                    service == Hosters.RD_REMOTE || service == Hosters.TB_REMOTE || service == Hosters.PM_REMOTE ->
                        s.t("Remote download via {0}: no seeding needed.", debridBrand(service))
                    service == "rd" || service == "tb" || service == "pm" ->
                        s.t("Unrestrict via {0} only.", debridBrand(service))
                    service == Hosters.DIRECT -> s.t("Direct links + Debrid (for configured tokens) with resume.")
                    else -> s.t("Resolve via {0} to a direct link.", Hosters.label(service))
                }
                Text(
                    svcHint,
                    color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                )
                if (kind == "HTTP") {
                    OutlinedTextField(
                        fileName, { fileName = it }, label = { Text(s.t("File name")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.t("Folder: "), style = MaterialTheme.typography.bodySmall)
                    Text(
                        folderLabel, style = MaterialTheme.typography.bodySmall,
                        color = HydraColors.Body, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onPickFolder) { Text(s.t("Change")) }
                }
            }
        },
        confirmButton = {
            HydraButton(if (starting) s.t("Starting…") else s.t("Download"), {
                if (chosenUri.isBlank()) return@HydraButton
                starting = true
                askNotif()
                scope.launch {
                    try {
                        val r = repack ?: GameRepack(
                            id = entity.id, title = entity.title, fileSize = entity.fileSize,
                            uris = listOf(chosenUri), downloadSourceId = "", downloadSourceName = entity.sourceName
                        )
                        repo.startTransfer(entity.shop, entity.objectId, r, kind, chosenUri, fileName.ifBlank { null }, service)
                        DownloadService.cmd(ctx, DownloadService.ACTION_START, entity.id)
                        onStarted()
                    } catch (_: Exception) {
                        starting = false
                    }
                }
            }, kind = "primary", enabled = !starting && chosenUri.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !starting) { Text(s.t("Cancel")) } }
    )
}

/** Выбор файлов торрента (приоритеты): отмеченные качаются, остальные пропускаются. */
@Composable
fun TorrentFilesDialog(infoHash: String, onDismiss: () -> Unit) {
    val s = ls()
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<TorrentEngine.TorrentFileEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var applying by remember { mutableStateOf(false) }

    LaunchedEffect(infoHash) {
        scope.launch {
            val list = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                TorrentEngine.listFiles(infoHash)
            }
            files = list
            selected = list.filter { it.selected }.map { it.index }.toSet()
            loading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = HydraColors.DarkBackground) {
            Column(Modifier.padding(16.dp).heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.t("Torrent files"), style = MaterialTheme.typography.headlineSmall)
                when {
                    loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = HydraColors.SecondaryText60)
                    }
                    files.isEmpty() -> HydraEmptyState(
                        title = s.t("Empty for now"),
                        hint = s.t("Files will appear once the download starts")
                    )
                    else -> {
                        Text(s.t("Selected: {0} of {1}", selected.size, files.size), color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
                        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(files, key = { it.index }) { f ->
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        selected = if (f.index in selected) selected - f.index else selected + f.index
                                    }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = f.index in selected,
                                        onCheckedChange = {
                                            selected = if (it) selected + f.index else selected - f.index
                                        }
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(f.path.substringAfterLast("/"), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${DownloadFolder.formatBytes(f.size)} · ${f.path.substringBeforeLast("/", "").ifBlank { "/" }}",
                                            color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        HydraButton(if (applying) s.t("Applying…") else s.t("Apply"), {
                            applying = true
                            scope.launch {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    TorrentEngine.setFileSelection(infoHash, selected)
                                }
                                onDismiss()
                            }
                        }, kind = "primary", enabled = !applying && files.isNotEmpty(), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** Локальные файлы завершённой загрузки: открыть / распаковать архив. */
@Composable
fun LocalFilesDialog(
    dir: File,
    onExtract: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val s = ls()
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    LaunchedEffect(dir) {
        files = try {
            dir.walkTopDown().maxDepth(2).filter { it.isFile }.toList().sortedBy { it.name.lowercase() }.take(200)
        } catch (_: Exception) { emptyList() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = HydraColors.DarkBackground) {
            Column(Modifier.padding(16.dp).heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(s.t("Files"), style = MaterialTheme.typography.headlineSmall)
                Text(dir.absolutePath, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (files.isEmpty()) {
                    HydraEmptyState(title = s.t("Folder is empty"), hint = s.t("Files will appear after the download completes"))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(files, key = { it.absolutePath }) { f ->
                            val isArc = ArchiveExtractor.isArchive(f.name)
                            ListItem(
                                headlineContent = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    Text(
                                        DownloadFolder.formatBytes(f.length()) + if (isArc) s.t(" · archive") else "",
                                        color = HydraColors.SecondaryText60
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        if (isArc) Icons.Default.Archive else Icons.AutoMirrored.Filled.InsertDriveFile,
                                        null, tint = HydraColors.SecondaryText60
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (isArc) onExtract(f)
                                    else openFileExternal(ctx, f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun openFileExternal(ctx: android.content.Context, file: File) {
    // Вне композиции: язык через синхронное зеркало LangStore (как для уведомлений сервиса).
    val s = try { Str(LangStore.peek(ctx)) } catch (_: Exception) { Str("en") }
    try {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        val mime = android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // resolveActivity требует <queries> в манифесте (API 30+), поэтому полагаемся
        // на перехват ActivityNotFoundException вместо предварительной проверки.
        ctx.startActivity(Intent.createChooser(i, s.t("Open")))
    } catch (_: Exception) {
        android.widget.Toast.makeText(ctx, s.t("Failed to open file"), android.widget.Toast.LENGTH_SHORT).show()
    }
}
