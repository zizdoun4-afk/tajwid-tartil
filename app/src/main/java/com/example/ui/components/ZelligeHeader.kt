package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ZelligeHeader(
    title: String,
    subtitle: String? = null,
    arabicTitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.92f)
                    )
                )
            )
            .padding(top = 12.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
    ) {
        // Geometric Zellige Interlace Overlay
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .padding(4.dp)
        ) {
            val width = size.width
            val height = size.height
            val goldColor = secondaryColor.copy(alpha = 0.18f)

            // Draw repeating 8-point geometric stars along border
            val starSize = 24f
            val spacing = 48f
            var x = 0f
            while (x < width) {
                drawStar8(
                    center = Offset(x, height - 12f),
                    size = starSize,
                    color = goldColor
                )
                drawStar8(
                    center = Offset(x + 24f, 12f),
                    size = starSize * 0.7f,
                    color = goldColor
                )
                x += spacing
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (navigationIcon != null) {
                    navigationIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                        )
                    }
                }

                if (arabicTitle != null) {
                    Text(
                        text = arabicTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (actions != null) {
                    actions()
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar8(
    center: Offset,
    size: Float,
    color: Color
) {
    val path = Path()
    // Square 1
    path.addRect(
        androidx.compose.ui.geometry.Rect(
            center.x - size / 2,
            center.y - size / 2,
            center.x + size / 2,
            center.y + size / 2
        )
    )
    drawPath(path, color, style = Stroke(width = 1.5f))

    // Square 2 rotated 45 degrees
    val path2 = Path()
    val r = size / 2
    path2.moveTo(center.x, center.y - r * 1.2f)
    path2.lineTo(center.x + r * 1.2f, center.y)
    path2.lineTo(center.x, center.y + r * 1.2f)
    path2.lineTo(center.x - r * 1.2f, center.y)
    path2.close()
    drawPath(path2, color, style = Stroke(width = 1.5f))
}
