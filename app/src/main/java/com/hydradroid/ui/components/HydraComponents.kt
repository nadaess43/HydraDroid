package com.hydradroid.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hydradroid.data.model.CatalogueSearchResult
import com.hydradroid.data.model.GameStats
import com.hydradroid.ui.theme.HydraColors

// ── Порт home.scss __cards: 1 колонка (<768) → 2 (768+) → 3 (1250+) → 4 (1600+).
// Карточка 180 всегда вытянута по горизонтали — никаких квадратов на телефоне.
@Composable
fun rememberHydraGridCells(): GridCells {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width < 768 -> GridCells.Fixed(1)
        width < 1250 -> GridCells.Fixed(2)
        width < 1600 -> GridCells.Fixed(3)
        else -> GridCells.Fixed(4)
    }
}

// ── Порт components/button: primary/outline/dark/cloud/danger, min-height 40, radius 8 ──
// button.scss: primary = светло-серый фон (#f0f1f7), НЕ teal. Teal — только акценты.
@Composable
fun HydraButton(
    text: String, onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: String = "primary", // primary|outline|dark|cloud|danger
    enabled: Boolean = true
) {
    if (kind == "cloud") {
        // button--cloud: linear-gradient(133deg, #14b9bb → #0a86d9), border #3dd6e9
        Box(
            modifier = modifier
                .height(40.dp)
                .alpha(if (enabled) 1f else HydraColors.DisabledOpacity)
                .clip(RoundedCornerShape(8.dp))
                .background(HydraColors.CloudButtonGradient)
                .border(BorderStroke(1.dp, Color(0xFF3DD6E9)), RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = HydraColors.Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp))
        }
        return
    }
    val bg = when (kind) {
        "outline" -> Color.Transparent
        "dark" -> HydraColors.DarkBackground
        "danger" -> HydraColors.Danger
        else -> HydraColors.Muted // primary
    }
    val fg = when (kind) {
        "primary" -> HydraColors.OnPrimaryLight
        else -> HydraColors.Muted
    }
    val border = when (kind) {
        "outline" -> BorderStroke(1.dp, HydraColors.Border)
        else -> null
    }
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp), border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = bg, contentColor = fg,
            disabledContainerColor = bg.copy(alpha = HydraColors.DisabledOpacity),
            disabledContentColor = fg.copy(alpha = HydraColors.DisabledOpacity)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

// ── Порт components/badge: blur-фон rgba(255,255,255,0.1), border 0.2, radius 6, 10–12px ──
@Composable
fun HydraBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(HydraColors.BadgeBackground, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, HydraColors.BadgeBorder), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            color = HydraColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ProtonBadge(tier: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0x26F50057), RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, Color(0x80F50057)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "ProtonDB · $tier", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFF7CA8), maxLines = 1
        )
    }
}

@Composable
fun StarRating(score: Double?, fontSize: Int = 12) {
    if (score == null) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(Icons.Default.Star, null, tint = HydraColors.Warning, modifier = Modifier.size(12.dp))
        Text("%.1f".format(score), color = HydraColors.Muted, fontSize = fontSize.sp, maxLines = 1)
    }
}

// ── game-card__specifics: иконка + число, muted. Компакт для узких карточек
// (в сетке 2 колонки карточка ~164dp — зазоры 8, иконки 12, шрифт 11).
@Composable
fun GameSpecifics(stats: GameStats?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Default.Download, null, tint = HydraColors.Muted, modifier = Modifier.size(12.dp))
            Text(stats?.downloadCount?.toString() ?: "…", color = HydraColors.Muted, fontSize = 11.sp, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Default.Group, null, tint = HydraColors.Muted, modifier = Modifier.size(12.dp))
            Text(stats?.playerCount?.toString() ?: "…", color = HydraColors.Muted, fontSize = 11.sp, maxLines = 1)
        }
        StarRating(stats?.averageScore, fontSize = 11)
    }
}

// ── Порт components/game-card: 100%x180 radius 4, border, тень, backdrop 0.7→transparent ──
// Телефон: метаданные всегда видны; бейджи — FlowRow в 1 строку (ничего не вылезает
// за карточку на узких экранах), статистика компактная.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameCard(
    game: CatalogueSearchResult,
    stats: GameStats? = null,
    onClick: () -> Unit,
    onAdd: (() -> Unit)? = null,
    showAddButton: Boolean = false,
    added: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth().height(180.dp)
            // Без shadow(): тень на каждой карточке сетки — главный источник лагов на слабых GPU.
            .clip(RoundedCornerShape(4.dp))
            .background(HydraColors.SkeletonBase)
            .border(BorderStroke(1.dp, HydraColors.Border), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = game.libraryImageUrl ?: game.coverImageUrl,
            contentDescription = game.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // backdrop gradient как .game-card__backdrop
        Box(modifier = Modifier.fillMaxSize().background(HydraColors.GameCardBackdrop))
        // content: padding 8x12, #dadbe1
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    game.title, color = HydraColors.TextBright, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                if (showAddButton && onAdd != null) {
                    IconButton(onClick = onAdd, enabled = !added, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (added) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = if (added) "В библиотеке" else "В библиотеку",
                            tint = if (added) HydraColors.Success else HydraColors.Muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            // download-options Badge max 2 + +N, строго в одну строку
            if (game.downloadSources.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxLines = 1
                ) {
                    game.downloadSources.take(2).forEach { HydraBadge(it) }
                    if (game.downloadSources.size > 2) HydraBadge("+${game.downloadSources.size - 2}")
                }
            }
            GameSpecifics(stats)
        }
    }
}

// ── Порт components/hero: full-bleed, backdrop 0.8→transparent, адаптивная высота ──
// Оригинал: 180 → 220 (≥480px) → 300 (≥768px) → 400 (десктоп). На телефоне 220, на планшете 300.
@Composable
fun HeroBanner(
    imageUrl: String?, logoUrl: String?, title: String, description: String?,
    onClick: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val heroHeight = if (maxWidth >= 600.dp) 300.dp else 220.dp
        val logoWidth = if (maxWidth >= 600.dp) 200.dp else 150.dp
        val contentPadding = if (maxWidth >= 600.dp) 24.dp else 16.dp
        Box(
            modifier = Modifier.fillMaxWidth().height(heroHeight)
                .background(HydraColors.SkeletonBase)
                .border(BorderStroke(1.dp, HydraColors.Border))
                .clickable(onClick = onClick)
        ) {
            if (imageUrl != null) AsyncImage(imageUrl, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(HydraColors.HeroBackdrop))
            Column(
                Modifier.align(Alignment.BottomStart).padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (logoUrl != null) AsyncImage(logoUrl, null, modifier = Modifier.width(logoWidth))
                else Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!description.isNullOrBlank()) Text(
                    description.take(200), color = HydraColors.Muted, fontSize = 13.sp, lineHeight = 20.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(1f, 0.55f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "skeletonAlpha")
    Box(
        modifier
            .fillMaxWidth().height(180.dp)            .shadow(8.dp, RoundedCornerShape(4.dp))            .clip(RoundedCornerShape(4.dp))
            .background(HydraColors.SkeletonBase)
            .alpha(alpha)
    )
}

// ── Порт LibraryGameCard: cover + favorite + удаление + СКАЧАТЬ ──
// Телефон: верх — коллекции/избранное; низ — название + скачать/убрать.
// Кнопка скачивания ведёт на страницу игры к репакам (для custom — открывает файл).
@Composable
fun LibraryCard(
    title: String, coverUrl: String?, favorite: Boolean,
    onClick: () -> Unit, onFavorite: () -> Unit, onRemove: () -> Unit,
    onCollections: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth().height(180.dp)            .shadow(8.dp, RoundedCornerShape(4.dp))            .clip(RoundedCornerShape(4.dp))
            .background(HydraColors.SkeletonBase)
            .border(BorderStroke(1.dp, HydraColors.Border), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        if (coverUrl != null) AsyncImage(coverUrl, title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(HydraColors.GameCardBackdrop))
        Row(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            if (onCollections != null) IconButton(onClick = onCollections, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.CollectionsBookmark, "Коллекции", tint = HydraColors.TextBright, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Избранное",
                    tint = if (favorite) Color.White else HydraColors.TextBright,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        // Низ: название + скачать + убрать — всё в одну строку, без вылезаний.
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title, color = HydraColors.TextBright, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            if (onDownload != null) IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Download, "Скачать", tint = HydraColors.BrandTeal, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", tint = HydraColors.TextBright, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Тёмная карточка контента (репаки/отзывы): фон #121212 + border 0.08 + radius 4 ──
@Composable
fun HydraCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = HydraColors.Background),
        border = BorderStroke(1.dp, HydraColors.Border),
        onClick = onClick ?: {}
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, count: Int? = null) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (count != null) Text("($count)", color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun HydraEmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(hint, color = HydraColors.SecondaryText60, style = MaterialTheme.typography.bodyMedium)
        if (action != null) {
            Spacer(Modifier.height(12.dp))
            action()
        }
    }
}
