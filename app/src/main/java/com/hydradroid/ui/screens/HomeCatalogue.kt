package com.hydradroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hydradroid.HydraDroidApp
import com.hydradroid.data.LibraryRepository
import com.hydradroid.data.model.CatalogueSearchResult
import com.hydradroid.data.remote.HydraApiClient
import com.hydradroid.ui.components.*
import com.hydradroid.ui.i18n.ls
import com.hydradroid.ui.theme.HydraColors
import kotlinx.coroutines.launch

private const val FIRST_PAGE = 12
private const val NEXT_PAGE = 24

private data class FeedState(
    val games: List<CatalogueSearchResult> = emptyList(),
    val hasMore: Boolean = true,
    val loadingMore: Boolean = false,
    val pageFailed: Boolean = false
)

// Порт pages/home: Hero + [hot|weekly|achievements] + Surprise me + заголовок + сетка.
// Телефон-порт (не копия): всё — один скролл, Hero уезжает вверх как в оригинале,
// лента бесконечная (автодогрузка при скролле), табы и «Мне повезёт» всегда видны.
@Composable
fun HomeScreen(
    onGame: (String, String) -> Unit,
    onSurprise: () -> Unit = {},
    surpriseRolling: Boolean = false
) {
    val s = ls()
    val ctx = LocalContext.current
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    var tab by remember { mutableStateOf("hot") }
    var refresh by remember { mutableStateOf(0) }
    var feeds by remember { mutableStateOf<Map<String, FeedState>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    // Источники пользователя (порт downloadSourceIds в /catalogue/{category}).
    // null = ещё не прочитаны из Room: первую страницу ждём, чтобы не кэшировать без фильтра.
    var sourceIds by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(Unit) {
        sourceIds = try { repo.enabledSourceIds() } catch (_: Exception) { emptyList() }
    }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val feed = feeds[tab] ?: FeedState()
    val games = feed.games
    val featured = feeds.values.flatMap { it.games }.firstOrNull() ?: games.firstOrNull()

    // Скролл к началу при смене таба.
    LaunchedEffect(tab) {
        gridState.scrollToItem(0)
    }

    // Первая страница таба.
    LaunchedEffect(tab, refresh, sourceIds) {
        if (feeds.containsKey(tab)) {
            loading = false
            return@LaunchedEffect
        }
        val ids = sourceIds ?: return@LaunchedEffect // ждём фильтр источников
        loading = true
        failed = false
        try {
            val list = HydraApiClient.service.getCatalogueCategory(tab, FIRST_PAGE, 0, ids)
            feeds = feeds + (tab to FeedState(games = list, hasMore = list.size >= FIRST_PAGE))
        } catch (_: Exception) {
            failed = true
        }
        loading = false
    }

    fun loadMore() {
        val requestTab = tab
        val cur = feeds[requestTab] ?: return
        if (!cur.hasMore || cur.loadingMore) return
        feeds = feeds + (requestTab to cur.copy(loadingMore = true, pageFailed = false))
        scope.launch {
            try {
                val list = HydraApiClient.service.getCatalogueCategory(requestTab, NEXT_PAGE, cur.games.size, sourceIds.orEmpty())
                val known = cur.games.map { "${it.shop}:${it.objectId}" }.toSet()
                val fresh = list.filter { "${it.shop}:${it.objectId}" !in known }
                val merged = cur.games + fresh
                // Ответ чужого таба (переключились во время запроса) — игнорируем.
                val latest = feeds[requestTab] ?: return@launch
                feeds = feeds + (requestTab to FeedState(
                    games = merged,
                    hasMore = list.size >= NEXT_PAGE && fresh.isNotEmpty(),
                    loadingMore = false
                ))
            } catch (_: Exception) {
                val latest = feeds[requestTab] ?: return@launch
                feeds = feeds + (requestTab to latest.copy(loadingMore = false, pageFailed = true))
            }
        }
    }

    // Автодогрузка: долистали почти до конца ленты.
    LaunchedEffect(gridState, tab) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { last ->
                val total = gridState.layoutInfo.totalItemsCount
                if (last != null && total > 0 && last >= total - 6) loadMore()
            }
    }

    val sectionTitle = when (tab) {
        "weekly" -> s.t("Top games of the week")
        "achievements" -> s.t("Games with achievements")
        else -> s.t("Trending now")
    }

    when {
        loading -> LazyVerticalGrid(
            rememberHydraGridCells(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { items(12) { SkeletonCard() } }

        failed && games.isEmpty() -> HydraEmptyState(
            title = s.t("Failed to load"),
            hint = s.t("Check your connection and try again"),
            action = {
                HydraButton(s.t("Retry"), {
                    feeds = feeds - tab
                    refresh++
                }, kind = "outline")
            },
            modifier = Modifier.fillMaxSize()
        )

        else -> LazyVerticalGrid(
            state = gridState,
            columns = rememberHydraGridCells(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero уезжает вверх вместе с лентой — как в оригинале.
            val f = featured
            if (f != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HeroBanner(
                        f.libraryImageUrl, null, f.title,
                        f.genres.joinToString(", ").ifBlank { null },
                        onClick = { onGame(f.shop, f.objectId) }
                    )
                }
            }
            // Табы + «Мне повезёт» всегда видны целиком (два ряда, без горизонтального обрезания).
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Официальные названия из ru-локали Hydra (без эмодзи — телефон).
                        listOf("hot" to s.t("Trending now"), "weekly" to s.t("Top games of the week"), "achievements" to s.t("Games with achievements")).forEach { (k, label) ->
                            HydraTabButton(
                                label, tab == k, { tab = k },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HydraButton(
                        if (surpriseRolling) s.t("Rolling…") else s.t("Surprise me"),
                        onSurprise,
                        kind = "outline", enabled = !surpriseRolling,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(sectionTitle, Modifier.padding(top = 4.dp), games.size.takeIf { games.isNotEmpty() })
            }
            items(games, key = { "${it.shop}:${it.objectId}" }, contentType = { "game" }) { g ->
                GameCard(g, onClick = { onGame(g.shop, g.objectId) })
            }
            // Футер бесконечности.
            item(span = { GridItemSpan(maxLineSpan) }) {
                when {
                    feed.loadingMore -> Box(
                        Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    feed.pageFailed -> Box(
                        Modifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HydraButton(s.t("Show more"), { loadMore() }, kind = "outline")
                    }
                    !feed.hasMore && games.isNotEmpty() -> Box(
                        Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.t("That's all"), color = HydraColors.SecondaryText60)
                    }
                }
            }
        }
    }
}

// CatalogueScreen переехал в CatalogueScreen.kt (пагинация + подсказки + жанры).
