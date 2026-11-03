package studio.tivaroid.treemivo.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.tivaroid.treemivo.data.DirectoryTile
import studio.tivaroid.treemivo.data.FileRecord
import studio.tivaroid.treemivo.data.directoryTiles

/**
 * Folder-first view: tiles represent *immediate children* of the current directory.
 * Tap a directory to drill down or a file to open its details. Metadata stays local.
 */
@Composable
fun DirectoryTreemapView(
    files: List<FileRecord>,
    directory: String,
    modifier: Modifier = Modifier,
    onOpenFolder: (String) -> Unit,
    onOpenFile: (FileRecord) -> Unit
) {
    val rectangles by produceState(
        initialValue = emptyList<TreemapRect<DirectoryTile>>(),
        files, directory
    ) {
        value = withContext(Dispatchers.Default) {
            buildTreemap(directoryTiles(files, directory), { it.sizeBytes })
        }
    }
    var zoom by remember(directory) { mutableFloatStateOf(1f) }
    var pan by remember(directory) { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current.density
    val label = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        }
    }

    fun constrain(offset: Offset, nextZoom: Float, width: Float, height: Float): Offset =
        Offset(
            offset.x.coerceIn(-width * (nextZoom - 1f), 0f),
            offset.y.coerceIn(-height * (nextZoom - 1f), 0f)
        )

    Canvas(modifier
        .pointerInput(rectangles, directory) {
            detectTransformGestures { centroid, translation, scale, _ ->
                val nextZoom = (zoom * scale).coerceIn(1f, 8f)
                val ratio = nextZoom / zoom
                pan = constrain(
                    Offset(
                        pan.x + translation.x + (centroid.x - pan.x) * (1f - ratio),
                        pan.y + translation.y + (centroid.y - pan.y) * (1f - ratio)
                    ),
                    nextZoom, size.width.toFloat(), size.height.toFloat()
                )
                zoom = nextZoom
            }
        }
        .pointerInput(rectangles, directory) {
            detectTapGestures(
                onDoubleTap = { touch ->
                    if (zoom > 1.02f) {
                        zoom = 1f
                        pan = Offset.Zero
                    } else {
                        zoom = 2.5f
                        pan = constrain(
                            Offset(touch.x * (1f - zoom), touch.y * (1f - zoom)),
                            zoom, size.width.toFloat(), size.height.toFloat()
                        )
                    }
                },
                onTap = { touch ->
                    val x = if (size.width > 0) (touch.x - pan.x) /
                        (zoom * size.width.toFloat()) else -1f
                    val y = if (size.height > 0) (touch.y - pan.y) /
                        (zoom * size.height.toFloat()) else -1f
                    rectangles.firstOrNull { tile ->
                        x >= tile.x && x < tile.x + tile.width &&
                            y >= tile.y && y < tile.y + tile.height
                    }?.item?.let { tile ->
                        if (tile.isFolder) onOpenFolder(tile.path)
                        else tile.file?.let(onOpenFile)
                    }
                }
            )
        }
    ) {
        drawRect(TreemivoBackground)
        clipRect {
            withTransform({
                translate(pan.x, pan.y)
                scale(zoom, pivot = Offset.Zero)
            }) {
                rectangles.forEachIndexed { index, tile ->
                    val left = tile.x * size.width
                    val top = tile.y * size.height
                    val width = tile.width * size.width
                    val height = tile.height * size.height
                    val base = tile.item.dominantKind.tileColor()
                    val darken = if (tile.item.isFolder) .82f else 1f
                    val shade = if (index % 3 == 0) .96f else 1f
                    drawRect(
                        Color(base.red * darken * shade,
                            base.green * darken * shade,
                            base.blue * darken * shade),
                        topLeft = Offset(left + .6f, top + .6f),
                        size = Size((width - 1.2f).coerceAtLeast(0f),
                            (height - 1.2f).coerceAtLeast(0f))
                    )
                    if (width > 74f * density && height > 34f * density) {
                        label.textSize = 12f * density
                        val title = if (tile.item.isFolder)
                            "▣ ${tile.item.name}" else tile.item.name
                        val maxWidth = width - 12f * density
                        val available = (title.length * maxWidth /
                            label.measureText(title).coerceAtLeast(1f))
                            .toInt().coerceIn(1, title.length)
                        val visible = if (available < title.length) {
                            title.take((available - 1).coerceAtLeast(1)) + "…"
                        } else title
                        drawContext.canvas.nativeCanvas.drawText(
                            visible, left + 6f * density,
                            top + 18f * density, label
                        )
                    }
                }
            }
        }
    }
}
