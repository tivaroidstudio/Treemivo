package studio.tivaroid.treemivo.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import studio.tivaroid.treemivo.data.FileKind
import studio.tivaroid.treemivo.data.FileRecord

val TreemivoBackground = Color(0xFF111317)
val TreemivoPanel = Color(0xFF1C2028)
val TreemivoSurface = Color(0xFF282D37)
val TreemivoAccent = Color(0xFF4BA5F9)
val TreemivoMuted = Color(0xFFABB3C2)

fun FileKind.tileColor(): Color = when (this) {
    FileKind.IMAGE -> Color(0xFFF59C32)
    FileKind.VIDEO -> Color(0xFF895BE0)
    FileKind.AUDIO -> Color(0xFF40A2FA)
    FileKind.DOCUMENT -> Color(0xFF6AC447)
    FileKind.ARCHIVE -> Color(0xFFF06059)
    FileKind.INSTALLER -> Color(0xFFDFBA2A)
    FileKind.OTHER -> Color(0xFF2AB7BC)
}

/** Realtime chart: every block corresponds to an actual file returned by the selected SAF provider. */
@Composable
fun TreemapView(
    files: List<FileRecord>,
    modifier: Modifier = Modifier,
    onSelect: (FileRecord) -> Unit
) {
    val tiles = remember(files) { buildTreemap(files, { it.sizeBytes }) }
    val scale = LocalDensity.current.density
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
    }
    Canvas(modifier = modifier.pointerInput(tiles) {
        detectTapGestures { tap ->
            val x = if (size.width > 0) tap.x / size.width else -1f
            val y = if (size.height > 0) tap.y / size.height else -1f
            tiles.firstOrNull {
                x >= it.x && x < it.x + it.width && y >= it.y && y < it.y + it.height
            }?.let { onSelect(it.item) }
        }
    }) {
        drawRect(TreemivoBackground)
        for ((index, tile) in tiles.withIndex()) {
            val left = tile.x * size.width
            val top = tile.y * size.height
            val width = tile.width * size.width
            val height = tile.height * size.height
            val shade = if (index % 4 == 0) 0.88f else if (index % 4 == 1) 0.96f else 1f
            val color = tile.item.kind.tileColor()
            drawRect(
                color = Color(color.red * shade, color.green * shade, color.blue * shade, 1f),
                topLeft = Offset(left + 1f, top + 1f),
                size = Size((width - 2f).coerceAtLeast(0f), (height - 2f).coerceAtLeast(0f))
            )
            if (width > 3f && height > 3f) {
                drawRect(
                    color = TreemivoBackground.copy(alpha = .42f),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 1f)
                )
            }
            if (width >= 110f * scale && height >= 50f * scale) {
                val text = tile.item.name.take(20)
                paint.textSize = 12f * scale
                paint.setShadowLayer(3f * scale, 0f, 1f * scale, android.graphics.Color.BLACK)
                val maxChars = (text.length * ((width - 12f * scale) / paint.measureText(text))).toInt()
                    .coerceIn(1, text.length)
                drawContext.canvas.nativeCanvas.drawText(
                    if (maxChars < text.length) text.take((maxChars - 1).coerceAtLeast(1)) + "…" else text,
                    left + 8f * scale, top + 20f * scale, paint
                )
            }
        }
    }
}
