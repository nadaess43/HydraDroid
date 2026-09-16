package com.hydradroid.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hydradroid.HydraDroidApp
import com.hydradroid.data.LibraryRepository
import com.hydradroid.data.local.saveSearchHistory
import com.hydradroid.data.local.searchHistoryFlow
import com.hydradroid.data.model.CatalogueSearchPayload
import com.hydradroid.data.model.CatalogueSearchResult
import com.hydradroid.data.remote.HydraApiClient
import com.hydradroid.ui.components.*
import com.hydradroid.ui.i18n.Str
import com.hydradroid.ui.i18n.ls
import com.hydradroid.ui.theme.HydraColors
import kotlinx.coroutines.launch

private const val PAGE = 30

// Порт pages/catalogue: result_count + sort + активные фильтры-чипы + сетка + пагинация.
// Мобильная адаптация: вместо десктопной пагинации и правой панели фильтров —
// кнопка «Показать ещё» и горизонтальный ряд жанров; сортировка с русскими метками.
private val SortLabels = mapOf(
    "popularity" to "Popularity",
    "releaseDate" to "Release date",
    "alphabetical" to "A-Z",
    "hydraScore" to "Hydra score"
)

// Официальный result_count: "{{resultCount}} результатов" с русским склонением.
private fun resultCountText(s: Str, count: Int): String {
    if (s.isRu) {
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "результат"
            count % 10 in 2..4 && (count % 100 < 12 || count % 100 > 14) -> "результата"
            else -> "результатов"
        }
        return "$count $form"
    }
    return if (count == 1) s.t("1 result") else s.t("{0} results", count)
}

@Composable
fun CatalogueScreen(query: String, onQueryChange: (String) -> Unit, onGame: (String, String) -> Unit) {
    val s = ls()
    val ctx = LocalContext.current
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    val scope = rememberCoroutineScope()
    var games by remember { mutableStateOf<List<CatalogueSearchResult>>(emptyList()) }
    var count by remember { mutableStateOf(0) }
    var sort by remember { mutableStateOf("popularity") }
    var genre by remember { mutableStateOf<String?>(null) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var skip by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<com.hydradroid.data.model.SearchSuggestion>>(emptyList()) }
    val history by searchHistoryFlow(ctx).collectAsState(initial = emptyList())
    var addedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Источники из настроек (порт downloadSourceFingerprints в теле запроса).
    var fingerprints by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        fingerprints = try { repo.enabledSourceFingerprints() } catch (_: Exception) { emptyList() }
    }

    // Запоминаем уже добавленные, чтобы иконка сразу была корректной.
    LaunchedEffect(games) {
        if (games.isNotEmpty()) {
            try {
                val keys = games
                    .filter { repo.isAdded(it.shop, it.objectId) }
                    .map { "${it.shop}:${it.objectId}" }.toSet()
                if (keys.isNotEmpty()) addedKeys += keys
            } catch (_: Exception) {}
        }
    }

    suspend fun load(reset: Boolean) {
        if (reset) { loading = true; skip = 0 } else loadingMore = true
        try {
            val r = HydraApiClient.service.searchCatalogue(
                CatalogueSearchPayload(
                    title = query, sortBy = sort, take = PAGE, skip = if (reset) 0 else skip,
                    genres = genre?.let { listOf(it) } ?: emptyList(),
                    downloadSourceFingerprints = fingerprints
                )
            )
            games = if (reset) r.edges else games + r.edges
            count = r.count
            if (!reset) skip += PAGE else skip = PAGE
        } catch (_: Exception) { }
        loading = false; loadingMore = false
    }

    LaunchedEffect(query, sort, genre) { load(true) }
    LaunchedEffect(Unit) {
        try { genres = HydraApiClient.service.getGenres().take(30) } catch (_: Exception) {}
    }
    // Подсказки при вводе от 2 символов (debounce уже в header — 300мс).
    // В историю пишем только выбранную подсказку (pickSuggestion), а не каждый
    // промежуточный ввод — иначе история забивается обрывками.
    LaunchedEffect(query) {
        suggestions = if (query.length >= 2) {
            try { HydraApiClient.service.getSearchSuggestions(query, 5) } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    fun pickSuggestion(s: String) {
        scope.launch { saveSearchHistory(ctx, s) }
        onQueryChange(s)
    }

    Column(Modifier.fillMaxSize()) {
        // catalogue__header: result_count слева, sort справа
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (loading) s.t("Search…") else resultCountText(s, count),
                color = HydraColors.Body, style = MaterialTheme.typography.bodyMedium
            )
            var expanded by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { expanded = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HydraColors.Muted)
            ) { Text(s.t(SortLabels[sort] ?: sort)) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                SortLabels.forEach { (k, label) ->
                    DropdownMenuItem(text = { Text(s.t(label)) }, onClick = { sort = k; expanded = false })
                }
            }
        }

        // Активные фильтры-чипы (порт FilterItem + clear-all), компактно для телефона
        val hasActive = query.isNotBlank() || genre != null
        if (hasActive) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (query.isNotBlank()) FilterChip(
                    selected = true,
                    onClick = { onQueryChange("") },
                    label = { Text("«$query»") },
                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                )
                if (genre != null) FilterChip(
                    selected = true,
                    onClick = { genre = null },
                    label = { Text(genre!!) },
                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                )
                TextButton(onClick = { onQueryChange(""); genre = null }) {
                    Text(s.t("Clear"), color = HydraColors.SecondaryText60)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Подсказки + история как SearchDropdown
        if (suggestions.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { sg -> AssistChip(onClick = { pickSuggestion(sg.title) }, label = { Text(sg.title) }) }
            }
            Spacer(Modifier.height(4.dp))
        } else if (query.isEmpty() && history.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.t("Recent searches:"), color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodySmall)
                history.take(8).forEach { h -> AssistChip(onClick = { onQueryChange(h) }, label = { Text(h) }) }
            }
            Spacer(Modifier.height(4.dp))
        }
        // Фильтр жанров
        if (genres.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(genre == null, { genre = null }, { Text(s.t("All")) })
                genres.forEach { g -> FilterChip(genre == g, { genre = if (genre == g) null else g }, { Text(g) }) }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (loading) {
            LazyVerticalGrid(
                rememberHydraGridCells(), contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { items(12) { SkeletonCard() } }
        } else if (games.isEmpty()) {
            HydraEmptyState(
                title = s.t("Nothing found"),
                hint = s.t("Try changing your search or filters"),
                action = { HydraButton(s.t("Reset"), { onQueryChange(""); genre = null }, kind = "outline") }
            )
        } else LazyVerticalGrid(
            rememberHydraGridCells(), modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(games, key = { "${it.shop}:${it.objectId}" }, contentType = { "game" }) { g ->
                val key = "${g.shop}:${g.objectId}"
                GameCard(
                    g, onClick = { onGame(g.shop, g.objectId) },
                    showAddButton = true, added = key in addedKeys,
                    onAdd = { scope.launch { repo.add(g); addedKeys += key } }
                )
            }
            if (games.size < count) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HydraButton(
                        if (loadingMore) s.t("Loading…") else s.t("Show more ({0})", count - games.size),
                        { scope.launch { load(false) } },
                        kind = "outline", enabled = !loadingMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
