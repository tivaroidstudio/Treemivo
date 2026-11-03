package studio.tivaroid.treemivo.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.CancellationSignal
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.tivaroid.treemivo.data.FileKind
import studio.tivaroid.treemivo.data.FileRecord
import java.nio.charset.StandardCharsets

/** Lightweight local preview; never uploads content or loads an entire full-resolution photo. */
@Composable
fun FilePreview(file: FileRecord, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preview by produceState<PreviewContent>(initialValue = PreviewContent.Empty, file.uri) {
        value = withContext(Dispatchers.IO) {
            loadPreview(context.contentResolver, file)
        }
    }
    Box(modifier.fillMaxWidth().heightIn(min = 80.dp, max = 290.dp)
        .background(TreemivoBackground, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center) {
        when (val p = preview) {
            is PreviewContent.Picture -> Image(
                bitmap = p.bitmap.asImageBitmap(),
                contentDescription = file.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(260.dp)
            )
            is PreviewContent.TextContent -> Text(
                text = p.value, fontSize = 12.sp, color = TreemivoMuted,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                maxLines = 10
            )
            PreviewContent.Empty -> Text(file.mimeType, color = TreemivoMuted,
                modifier = Modifier.padding(12.dp))
        }
    }
}

private sealed interface PreviewContent {
    data object Empty : PreviewContent
    data class Picture(val bitmap: Bitmap) : PreviewContent
    data class TextContent(val value: String) : PreviewContent
}

private fun loadPreview(resolver: ContentResolver, file: FileRecord): PreviewContent {
    try {
        if (file.kind == FileKind.IMAGE || file.kind == FileKind.VIDEO) {
            if (Build.VERSION.SDK_INT >= 29) {
                val bitmap = resolver.loadThumbnail(file.uri, Size(640, 400), null as CancellationSignal?)
                return PreviewContent.Picture(bitmap)
            }
            if (file.kind == FileKind.IMAGE) {
                resolver.openInputStream(file.uri)?.use { input ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 8 }
                    BitmapFactory.decodeStream(input, null, options)?.let {
                        return PreviewContent.Picture(it)
                    }
                }
            }
        }
        if (file.name.endsWith(".pdf", ignoreCase = true) || file.mimeType == "application/pdf") {
            resolver.openFileDescriptor(file.uri, "r")?.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (renderer.pageCount > 0) renderer.openPage(0).use { page ->
                        val width = 540
                        val height = (width * page.height.toFloat() / page.width.toFloat())
                            .toInt().coerceIn(1, 760)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        return PreviewContent.Picture(bitmap)
                    }
                }
            }
        }
        val isText = file.mimeType.startsWith("text/") ||
            file.name.substringAfterLast('.', "").lowercase() in
                setOf("txt", "md", "json", "xml", "csv", "kt", "py", "js")
        if (isText && file.sizeBytes <= 2L * 1024 * 1024) {
            resolver.openInputStream(file.uri)?.use { stream ->
                val buffer = ByteArray(2048)
                val count = stream.read(buffer)
                if (count > 0) {
                    val content = String(buffer, 0, count, StandardCharsets.UTF_8)
                    return PreviewContent.TextContent(content.filter { it == '\n' || it == '\t' || it >= ' ' })
                }
            }
        }
    } catch (_: Exception) {
        // A document provider can deny preview while still allowing metadata.
    }
    return PreviewContent.Empty
}
