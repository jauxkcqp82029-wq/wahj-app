package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WahjBlue
import com.example.ui.theme.WahjOnlineGreen
import com.example.ui.theme.WahjOnlineGreenSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Minimalist line-art logo of a mother embracing her child inside a protective arc.
 */
@Composable
fun WahjLineArtLogo(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    primaryStrokeColor: Color = MaterialTheme.colorScheme.onBackground,
    accentStrokeColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeMain = Stroke(width = w * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val strokeAccent = Stroke(width = w * 0.038f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Outer protective cobalt blue arc
        drawArc(
            color = accentStrokeColor,
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            style = strokeAccent
        )

        // Mother's head (gentle tilt towards child)
        drawCircle(
            color = primaryStrokeColor,
            radius = w * 0.115f,
            center = Offset(w * 0.44f, h * 0.30f),
            style = strokeMain
        )

        // Mother's embracing shoulder and arm curve
        val motherArm = Path().apply {
            moveTo(w * 0.25f, h * 0.68f)
            cubicTo(
                w * 0.22f, h * 0.46f,
                w * 0.34f, h * 0.42f,
                w * 0.46f, h * 0.46f
            )
            cubicTo(
                w * 0.66f, h * 0.50f,
                w * 0.74f, h * 0.64f,
                w * 0.56f, h * 0.76f
            )
            cubicTo(
                w * 0.44f, h * 0.82f,
                w * 0.31f, h * 0.78f,
                w * 0.25f, h * 0.68f
            )
        }
        drawPath(path = motherArm, color = primaryStrokeColor, style = strokeMain)

        // Child's head nestled in mother's embrace
        drawCircle(
            color = accentStrokeColor,
            radius = w * 0.082f,
            center = Offset(w * 0.60f, h * 0.48f),
            style = strokeAccent
        )

        // Child's body curve
        val childBody = Path().apply {
            moveTo(w * 0.51f, h * 0.60f)
            cubicTo(
                w * 0.54f, h * 0.69f,
                w * 0.62f, h * 0.72f,
                w * 0.69f, h * 0.66f
            )
        }
        drawPath(path = childBody, color = accentStrokeColor, style = strokeAccent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WahjTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun WahjCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun WahjStatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    WahjCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WahjStatusBadge(
    text: String,
    isPositive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bg = if (isPositive) WahjOnlineGreenSoft else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isPositive) WahjOnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

fun formatMinutesArabic(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hrs = safe / 60
    val rem = safe % 60
    return when {
        hrs > 0 && rem > 0 -> "$hrs س $rem د"
        hrs > 0 -> "$hrs ساعة"
        else -> "$rem دقيقة"
    }
}

fun formatTimestampArabic(epochMs: Long): String {
    val diffSec = ((System.currentTimeMillis() - epochMs) / 1000L).coerceAtLeast(0)
    return when {
        diffSec < 60 -> "الآن"
        diffSec < 3600 -> "منذ ${diffSec / 60} دقيقة"
        diffSec < 86400 -> "منذ ${diffSec / 3600} ساعة"
        else -> SimpleDateFormat("dd/MM HH:mm", Locale("ar")).format(Date(epochMs))
    }
}

fun formatClockTime(epochMs: Long): String {
    return SimpleDateFormat("HH:mm", Locale("ar")).format(Date(epochMs))
}
