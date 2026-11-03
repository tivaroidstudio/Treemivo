package studio.tivaroid.treemivo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.tivaroid.treemivo.R
import studio.tivaroid.treemivo.data.FileRecord
import studio.tivaroid.treemivo.data.StorageInsights
import studio.tivaroid.treemivo.data.analyzeStorageInsights
import java.util.Locale

@Composable
fun StorageInsightsPanel(
    files: List<FileRecord>,
    selectedExtension: String?,
    onExtension: (String?) -> Unit,
    onFile: (FileRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val insights by produceState<StorageInsights?>(null, files) {
        value = withContext(Dispatchers.Default) { analyzeStorageInsights(files) }
    }
    val result = insights
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.insights_title),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.insights_scope),
            color = TreemivoMuted, fontSize = 12.sp)
        if (result == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCell(stringResource(R.string.insights_zero_files),
                result.zeroByteFiles.toString(), Modifier.weight(1f))
            MetricCell(stringResource(R.string.insights_huge_files),
                result.veryLargeFiles.toString(), Modifier.weight(1f))
            MetricCell(stringResource(R.string.insights_hidden_files),
                result.hiddenFiles.toString(), Modifier.weight(1f))
        }
        Text(stringResource(R.string.insights_extensions), fontWeight = FontWeight.SemiBold)
        result.extensions.forEach { entry ->
            val title = if (entry.extension == "(none)")
                stringResource(R.string.insights_no_extension) else ".${entry.extension}"
            Surface(color = TreemivoSurface, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable {
                    onExtension(if (selectedExtension == entry.extension) null else entry.extension)
                }) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f),
                            fontWeight = if (selectedExtension == entry.extension)
                                FontWeight.Bold else FontWeight.Normal)
                        Text(humanSize(entry.bytes) + " · " + entry.files, fontSize = 12.sp,
                            color = TreemivoMuted)
                    }
                    LinearProgressIndicator(
                        progress = {
                            if (result.indexedBytes > 0L)
                                (entry.bytes.toFloat() / result.indexedBytes).coerceIn(0f, 1f)
                            else 0f
                        },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = TreemivoAccent,
                        trackColor = TreemivoPanel
                    )
                }
            }
        }
        if (selectedExtension != null) TextButton(onClick = { onExtension(null) }) {
            Text(stringResource(R.string.insights_clear_extension))
        }
        Text(stringResource(R.string.insights_file_age), fontWeight = FontWeight.SemiBold)
        AgeLine(stringResource(R.string.insights_last_week), result.age.recentBytes, result.indexedBytes)
        AgeLine(stringResource(R.string.insights_last_month), result.age.lastMonthBytes, result.indexedBytes)
        AgeLine(stringResource(R.string.insights_older), result.age.olderBytes, result.indexedBytes)
        AgeLine(stringResource(R.string.insights_date_unknown), result.age.unknownBytes, result.indexedBytes)
        Text(stringResource(R.string.insights_largest_files), fontWeight = FontWeight.SemiBold)
        result.largestFiles.forEach { file ->
            Row(Modifier.fillMaxWidth().background(TreemivoSurface, RoundedCornerShape(9.dp))
                .clickable { onFile(file) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(file.name, Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                Spacer(Modifier.width(7.dp))
                Text(humanSize(file.sizeBytes), fontSize = 12.sp, color = TreemivoMuted)
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = TreemivoSurface) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, fontWeight = FontWeight.Bold, color = TreemivoAccent,
                style = MaterialTheme.typography.titleMedium)
            Text(label, color = TreemivoMuted, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun AgeLine(label: String, bytes: Long, total: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(120.dp), color = TreemivoMuted, fontSize = 12.sp)
        LinearProgressIndicator(
            progress = { if (total > 0L) (bytes.toFloat() / total).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.weight(1f).height(6.dp),
            color = TreemivoAccent, trackColor = TreemivoSurface
        )
        Spacer(Modifier.width(8.dp))
        Text(humanSize(bytes), fontSize = 11.sp)
    }
}

private fun humanSize(value: Long): String {
    var bytes = value.coerceAtLeast(0).toDouble()
    val suffix = arrayOf("B", "KB", "MB", "GB", "TB")
    var index = 0
    while (bytes >= 1024.0 && index < suffix.lastIndex) {
        bytes /= 1024.0
        index++
    }
    return String.format(Locale.getDefault(), "%.1f %s", bytes, suffix[index])
}
