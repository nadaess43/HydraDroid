package com.hydradroid.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.hydradroid.data.local.DownloadEntity
import com.hydradroid.data.local.DownloadFolder
import com.hydradroid.data.model.GameRepack
import com.hydradroid.data.torrent.TorrentEngine
import com.hydradroid.service.DownloadService
import com.hydradroid.ui.components.HydraButton
import com.hydradroid.ui.components.HydraEmptyState
import com.hydradroid.ui.theme.HydraColors
import kotlinx.coroutines.launch
import java.io.File

// ── Общие трансферные UI: диалог запуска, файлы торрента, локальные файлы ──

fun detectKind(uri: String): String = when {
    uri.startsWith("magnet:") -> "TORRENT"
    uri.lowercase().substringBefore("?").endsWith(".torrent") -> "TORRENT"
    else -> "HTTP"
}

// Официальные статусы из ru-локали (downloads).
fun statusLabel(status: String): String = when (status) {
    "queued" -> "В очереди"
    "fetching" -> "Загрузка метаданных…"
    "downloading" -> "Скачивание"
    "paused" -> "Приостановлено"
    "seeding" -> "Раздача"
    "complete" -> "Завершено"
    "error" -> "Ошибка"
    else -> status
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
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    val scope = rememberCoroutineScope()
    val askNotif = rememberNotifPermission()

    val uris = remember(repack, entity) {
        val all = (repack?.uris ?: emptyList()) + (repack?.unavailableUris ?: emptyList())
        (if (all.isNotEmpty()) all.distinct() else listOf(entity.uri)).filter { it.isNotBlank() }
    }
    var chosenUri by remember { mutableStateOf(uris.firstOrNull { it.startsWith("magnet:") } ?: uris.firstOrNull() ?: "") }
    var kind by remember { mutableStateOf(detectKind(chosenUri)) }
    var fileName by remember { mutableStateOf(entity.fileName?.ifBlank { null } ?: chosenUri.substringAfterLast("/").substringBefore("?").ifBlank { "${entity.title}.bin" }) }
    var starting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!starting) onDismiss() },
        title = { Text("Скачать", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entity.title, fontWeight = FontWeight.Bold)
                Text("${entity.sourceName} · ${entity.fileSize ?: "?"}", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
                if (uris.size > 1) {
                    Text("Источник (${uris.size})", style = MaterialTheme.typography.bodySmall)
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
                                onClick = { chosenUri = u; kind = detectKind(u); exp = false }
                            )
                        }
                    }
                }
                // Тип движка (автоопределён, можно переключить вручную)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(kind == "TORRENT", { kind = "TORRENT" }, { Text("Торрент") })
                    FilterChip(kind == "HTTP", { kind = "HTTP" }, { Text("HTTP") })
                }
                if (kind == "TORRENT") {
                    Text(
                        "Качается через торренты: с докачкой и раздачей.",
                        color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    OutlinedTextField(
                        fileName, { fileName = it }, label = { Text("Имя файла") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Text(
                        "Прямые ссылки + Debrid (по заполненным токенам) с докачкой.",
                        color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Папка: ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        folderLabel, style = MaterialTheme.typography.bodySmall,
                        color = HydraColors.Body, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onPickFolder) { Text("Изменить") }
                }
            }
        },
        confirmButton = {
            HydraButton(if (starting) "Старт…" else "Скачать", {
                if (chosenUri.isBlank()) return@HydraButton
                starting = true
                askNotif()
                scope.launch {
                    try {
                        val r = repack ?: GameRepack(
                            id = entity.id, title = entity.title, fileSize = entity.fileSize,
                            uris = listOf(chosenUri), downloadSourceId = "", downloadSourceName = entity.sourceName
                        )
                        repo.startTransfer(entity.shop, entity.objectId, r, kind, chosenUri, fileName.ifBlank { null })
                        DownloadService.cmd(ctx, DownloadService.ACTION_START, entity.id)
                        onStarted()
                    } catch (_: Exception) {
                        starting = false
                    }
                }
            }, kind = "primary", enabled = !starting && chosenUri.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !starting) { Text("Отмена") } }
    )
}

/** Выбор файлов торрента (приоритеты): отмеченные качаются, остальные пропускаются. */
@Composable
fun TorrentFilesDialog(infoHash: String, onDismiss: () -> Unit) {
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
                Text("Файлы торрента", style = MaterialTheme.typography.headlineSmall)
                when {
                    loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = HydraColors.SecondaryText60)
                    }
                    files.isEmpty() -> HydraEmptyState(
                        title = "Пока пусто",
                        hint = "Файлы появятся, когда начнётся загрузка"
                    )
                    else -> {
                        Text("Выбрано: ${selected.size} из ${files.size}", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
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
                        HydraButton(if (applying) "Применение…" else "Применить", {
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
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    LaunchedEffect(dir) {
        files = try {
            dir.walkTopDown().maxDepth(2).filter { it.isFile }.toList().sortedBy { it.name.lowercase() }.take(200)
        } catch (_: Exception) { emptyList() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = HydraColors.DarkBackground) {
            Column(Modifier.padding(16.dp).heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Файлы", style = MaterialTheme.typography.headlineSmall)
                Text(dir.absolutePath, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (files.isEmpty()) {
                    HydraEmptyState(title = "Папка пуста", hint = "Файлы появятся после завершения загрузки")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(files, key = { it.absolutePath }) { f ->
                            val isArc = ArchiveExtractor.isArchive(f.name)
                            ListItem(
                                headlineContent = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    Text(
                                        DownloadFolder.formatBytes(f.length()) + if (isArc) " · архив" else "",
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
    try {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        val mime = android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // resolveActivity требует <queries> в манифесте (API 30+), поэтому полагаемся
        // на перехват ActivityNotFoundException вместо предварительной проверки.
        ctx.startActivity(Intent.createChooser(i, "Открыть"))
    } catch (_: Exception) {
        android.widget.Toast.makeText(ctx, "Не удалось открыть файл", android.widget.Toast.LENGTH_SHORT).show()
    }
}
