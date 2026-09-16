package com.hydradroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.compose.*
import com.hydradroid.data.LibraryRepository
import com.hydradroid.data.local.saveSearchHistory
import com.hydradroid.data.local.searchHistoryFlow
import com.hydradroid.data.remote.HydraApiClient
import com.hydradroid.ui.Routes
import com.hydradroid.ui.screens.*
import com.hydradroid.ui.theme.HydraColors
import com.hydradroid.ui.theme.HydraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthDeepLink(intent)
        setContent { HydraTheme { HydraApp() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthDeepLink(intent)
    }

    // Порт handleExternalAuth: hydradroid://auth?accessToken=..&refreshToken=..
    private fun handleAuthDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "hydradroid" && uri.host == "auth") {
            val access = uri.getQueryParameter("accessToken") ?: return
            val refresh = uri.getQueryParameter("refreshToken") ?: ""
            com.hydradroid.data.local.TokenStore.saveTokens(this, access, refresh)
            HydraApiClient.accessToken = access
        }
    }
}

/** Открыть страницу входа Hydra в Custom Tab (порт AuthWindow). */
fun openSignIn(ctx: android.content.Context) {
    val url = com.hydradroid.BuildConfig.HYDRA_AUTH_URL + "?redirect=hydradroid://auth"
    CustomTabsIntent.Builder().build().launchUrl(ctx, url.toUri())
}

// Порт app.tsx App layout: Sidebar + Header + Outlet + BottomPanel.
// Десктоп Sidebar 250px → на телефоне BottomBar (тот же порядок, active = rgba white 0.1,
// иконки приглушённые как sidebar__menu-item, без крикливых акцентов).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydraApp() {
    val nav = rememberNavController()
    val backstack by nav.currentBackStackEntryAsState()
    val route = backstack?.destination?.route ?: Routes.HOME
    var search by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    LaunchedEffect(search) { delay(300); debounced = search } // debounce как catalogue/header
    val scope = rememberCoroutineScope()
    var surpriseRolling by remember { mutableStateOf(false) }
    val appCtx = LocalContext.current

    // Прогрев кубика при старте: списки Steam250 грузятся заранее,
    // первое нажатие срабатывает мгновенно.
    LaunchedEffect(Unit) {
        scope.launch {
            try { com.hydradroid.data.random.RandomGameRoller.warmup(appCtx.cacheDir) }
            catch (_: Exception) {}
        }
    }

    // Порт getRandomGame + steam-250.ts 1:1: перетасованные списки Steam250,
    // выдача по кругу без повторов. Если Steam250 недоступен — честный фолбэк:
    // случайная игра со всего каталога. Текущая страница заменяется, стек не растёт.
    fun rollRandom() {
        if (surpriseRolling) return
        surpriseRolling = true
        scope.launch {
            try {
                val pick = com.hydradroid.data.random.RandomGameRoller.next(appCtx.cacheDir)
                if (pick != null) {
                    nav.navigate(Routes.game("steam", pick.objectId)) {
                        popUpTo(Routes.GAME) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    // Фолбэк: случайная со всего каталога.
                    val probe = HydraApiClient.service.searchCatalogue(
                        com.hydradroid.data.model.CatalogueSearchPayload(title = "", take = 5, skip = 0)
                    )
                    if (probe.count > 0) {
                        val skip = (0 until probe.count).random()
                        val fb = HydraApiClient.service.searchCatalogue(
                            com.hydradroid.data.model.CatalogueSearchPayload(title = "", take = 5, skip = skip)
                        ).edges.randomOrNull()
                        if (fb != null) nav.navigate(Routes.game(fb.shop, fb.objectId)) {
                            popUpTo(Routes.GAME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                surpriseRolling = false
            }
        }
    }

    // Порядок и смысл — как sidebar-меню оригинала.
    val tabs = listOf(
        Triple(Routes.HOME, "Главная", Icons.Default.Home),
        Triple(Routes.CATALOGUE, "Каталог", Icons.Default.Apps),
        Triple(Routes.LIBRARY, "Библиотека", Icons.Default.Book),
        Triple(Routes.DOWNLOADS, "Загрузки", Icons.Default.Download),
        Triple(Routes.SETTINGS, "Настройки", Icons.Default.Settings),
    )

    Scaffold(
        topBar = {
            // Порт components/header: back + title; поиск — второй строкой на всю ширину
            // (на телефоне 200px в одну строку с заголовком не влезают).
            HydraHeader(
                route = route,
                search = search,
                onSearchChange = {
                    search = it.take(255)
                    if (it.isNotEmpty() && route != Routes.CATALOGUE) {
                        nav.navigate(Routes.CATALOGUE) { launchSingleTop = true }
                    }
                },
                onBack = { nav.popBackStack() },
                onNotifications = { nav.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true } }
            )
        },
        bottomBar = {
            Column {
                // Порт BottomPanel: статус очереди → navigate(/downloads)
                BottomPanelStrip(route = route, onClick = { nav.navigate(Routes.DOWNLOADS) { launchSingleTop = true } })
                NavigationBar(containerColor = HydraColors.DarkBackground) {
                    tabs.forEach { (r, label, icon) ->
                        val selected = route.startsWith(r)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(r) {
                                    launchSingleTop = true
                                    popUpTo(Routes.HOME)
                                }
                            },
                            icon = {
                                Icon(
                                    icon, null,
                                    tint = if (selected) HydraColors.Muted else HydraColors.SecondaryText60
                                )
                            },
                            label = {
                                Text(
                                    label, fontSize = 10.sp, maxLines = 1,
                                    color = if (selected) HydraColors.Muted else HydraColors.SecondaryText60
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HydraColors.Muted,
                                unselectedIconColor = HydraColors.SecondaryText60,
                                indicatorColor = HydraColors.ActiveMenuItem
                            )
                        )
                    }
                }
            }
        },
        containerColor = HydraColors.Background
    ) { pad ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(pad)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onGame = { s, id -> nav.navigate(Routes.game(s, id)) },
                    onSurprise = { rollRandom() },
                    surpriseRolling = surpriseRolling
                )
            }
            composable(Routes.CATALOGUE) { CatalogueScreen(query = debounced, onQueryChange = { search = it }, onGame = { s, id -> nav.navigate(Routes.game(s, id)) }) }
            composable(Routes.LIBRARY) { LibraryScreen(onGame = { s, id -> nav.navigate(Routes.game(s, id)) }) }
            composable(Routes.DOWNLOADS) { DownloadsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(onProfile = { nav.navigate(Routes.profile("me")) }, onNotifications = { nav.navigate(Routes.NOTIFICATIONS) }) }
            composable(Routes.GAME) { e ->
                GameDetailsScreen(shop = e.arguments?.getString("shop") ?: "steam", objectId = e.arguments?.getString("objectId") ?: "",
                    onAchievements = { s, id -> nav.navigate("achievements?objectId=$id&shop=$s") }, onBack = { nav.popBackStack() },
                    onRollRandom = { rollRandom() })
            }
            composable(Routes.PROFILE) { ProfileScreen() }
            composable(Routes.ACHIEVEMENTS) { AchievementsScreen() }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen() }
        }
    }
}

private fun headerTitle(route: String): String = when {
    route.startsWith(Routes.CATALOGUE) -> "Каталог"
    route.startsWith(Routes.LIBRARY) -> "Библиотека"
    route.startsWith(Routes.DOWNLOADS) -> "Загрузки"
    route.startsWith(Routes.SETTINGS) -> "Настройки"
    route.contains("game/") -> "Игра"
    route.contains("profile/") -> "Профиль"
    route.contains("achievements") -> "Достижения"
    route.contains("notifications") -> "Уведомления"
    else -> "Hydra"
}

/**
 * Порт header: back-кнопка + заголовок + действия, вторая строка — поиск
 * (header__search: bg #121212, radius 8, border 0.08, 40px; в фокусе border #dadbe1).
 * Подсказки — выпадающий список под полем (порт SearchDropdown):
 * история из DataStore + /catalogue/search/suggestions от 2 символов.
 */
@Composable
private fun HydraHeader(
    route: String,
    search: String,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    onNotifications: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val searchInteraction = remember { MutableInteractionSource() }
    val isFocused by searchInteraction.collectIsFocusedAsState()
    var suggestions by remember { mutableStateOf<List<com.hydradroid.data.model.SearchSuggestion>>(emptyList()) }
    val history by searchHistoryFlow(ctx).collectAsState(initial = emptyList())
    var searchTick by remember { mutableStateOf("") }
    LaunchedEffect(searchTick) {
        delay(300)
        suggestions = if (searchTick.length >= 2) {
            try { HydraApiClient.service.getSearchSuggestions(searchTick, 5) } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    Column(
        Modifier
            .background(HydraColors.DarkBackground)
            .border(BorderStroke(0.5.dp, HydraColors.Border))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (route.contains("game/") || route.contains("profile/") || route.contains("achievements") || route.contains("notifications")) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = HydraColors.Body)
                }
            }
            Text(
                headerTitle(route), style = MaterialTheme.typography.headlineSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNotifications, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Notifications, "Уведомления", tint = HydraColors.SecondaryText60, modifier = Modifier.size(20.dp))
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HydraColors.Background)
                        .border(
                            BorderStroke(1.dp, if (isFocused) HydraColors.TextBright else HydraColors.Border),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = HydraColors.SecondaryText50, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = search,
                        onValueChange = { onSearchChange(it); searchTick = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        interactionSource = searchInteraction,
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        textStyle = TextStyle(color = HydraColors.TextBright, fontSize = 14.sp),
                        decorationBox = { inner ->
                            if (search.isEmpty()) Text("Поиск", color = HydraColors.SecondaryText50, fontSize = 14.sp)
                            inner()
                        }
                    )
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange(""); searchTick = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Очистить", tint = HydraColors.SecondaryText50, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                // Выпадающие подсказки / история
                if (isFocused) {
                    if (suggestions.isNotEmpty()) {
                        SuggestionDrop(
                            items = suggestions.map { it.title to it.title },
                            onPick = {
                                scope.launch { saveSearchHistory(ctx, it) }
                                onSearchChange(it); searchTick = ""
                            }
                        )
                    } else if (search.isEmpty() && history.isNotEmpty()) {
                        SuggestionDrop(
                            items = history.take(5).map { it to it },
                            onPick = { onSearchChange(it); searchTick = "" }
                        )
                    }
                }
            }
        }
    }
}

/** Выпадающий список подсказок/истории под полем поиска. */
@Composable
private fun SuggestionDrop(items: List<Pair<String, String>>, onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HydraColors.DarkBackground)
            .border(BorderStroke(1.dp, HydraColors.Border), RoundedCornerShape(8.dp))
    ) {
        items.forEach { (key, label) ->
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onPick(key) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, null, tint = HydraColors.SecondaryText50, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, color = HydraColors.Body, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
/** Порт bottom-panel: центр — статус загрузок, справа — версия. Клик ведёт в загрузки. */
@Composable
private fun BottomPanelStrip(route: String, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LibraryRepository((ctx.applicationContext as HydraDroidApp).db) }
    // Реактивно: пересчёт только когда база реально изменилась.
    val queue by repo.queueFlow().collectAsState(initial = emptyList())
    val queueSize = queue.count { it.status != "complete" }
    Row(
        Modifier.fillMaxWidth()
            .background(HydraColors.Background)
            .border(BorderStroke(0.5.dp, HydraColors.Border))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (queueSize > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(HydraColors.BrandTeal))
                Text(
                    "В очереди: $queueSize",
                    color = HydraColors.Body, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text("Нет активных загрузок", color = HydraColors.SecondaryText60, fontSize = 12.sp)
        }
        Text("v${com.hydradroid.BuildConfig.VERSION_NAME}", color = HydraColors.SecondaryText50, fontSize = 12.sp)
    }
}
