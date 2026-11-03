package studio.tivaroid.treemivo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.tivaroid.treemivo.data.ScanSnapshot

/** Local aggregate-only history, plotted without collecting file names. */
@Composable
fun HistoryTrendChart(snapshots: List<ScanSnapshot>) {
    val series = remember(snapshots) {
        snapshots.asReversed().takeLast(30).map { it.bytes.coerceAtLeast(0L).toDouble() }
    }
    Column(Modifier.fillMaxWidth().background(TreemivoSurface, RoundedCornerShape(14.dp))
        .padding(12.dp)) {
        Text("Storage trend · indexed source", fontSize = 12.sp, color = TreemivoMuted)
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(108.dp)) {
            if (series.size < 2) return@Canvas
            val max = (series.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
            val min = series.minOrNull() ?: 0.0
            val range = (max - min).coerceAtLeast(1.0)
            val path = Path()
            series.forEachIndexed { index, value ->
                val x = index.toFloat() / (series.size - 1) * size.width
                val y = size.height - ((value - min) / range).toFloat() * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = TreemivoAccent, style = Stroke(width = 3.dp.toPx()))
        }
    }
}
