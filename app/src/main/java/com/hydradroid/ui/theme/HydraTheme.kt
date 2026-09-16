package com.hydradroid.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val HydraScheme = darkColorScheme(
    primary = HydraColors.Muted,          // button--primary: светло-серый фон
    onPrimary = HydraColors.OnPrimaryLight,
    secondary = HydraColors.BrandTeal,    // акцент только для выделений, не для заливок
    tertiary = HydraColors.PlatinumCyan,
    background = HydraColors.Background,
    surface = HydraColors.DarkBackground,
    surfaceVariant = HydraColors.SkeletonBase,
    onBackground = HydraColors.Body,
    onSurface = HydraColors.Muted,
    onSurfaceVariant = HydraColors.SecondaryText60,
    error = HydraColors.Error,
    outline = HydraColors.Border,
    outlineVariant = HydraColors.Border
)

// Шейпы 1:1: карточка/модалка 4, бейдж 6, кнопка/поиск 8.
private val HydraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

// Полный аналог app.scss body { Noto Sans, 14px, #d0d1d7 }
@Composable
fun HydraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HydraScheme, typography = HydraTypography, shapes = HydraShapes, content = content)
}
