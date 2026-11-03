package studio.tivaroid.treemivo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.tivaroid.treemivo.R
import studio.tivaroid.treemivo.data.StorageViewModel
import studio.tivaroid.treemivo.data.TrackedFileChange
import java.util.Locale

/** Pro review: only size changes in the same tracked URI among top 200 files. */
@Composable
fun ChangedFilesPanel(vm: StorageViewModel) {
    if (!vm.billing.isPro) return
    val changes by produceState(
        initialValue = emptyList<TrackedFileChange>(), vm.report, vm.history, vm.billing.isPro
    ) {
        value = withContext(Dispatchers.Default) { vm.changedLargeFiles() }
    }
    var preview by remember { mutableStateOf<TrackedFileChange?>(null) }
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = TreemivoPanel, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(stringResource(R.string.changed_files_title),
                style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.changed_files_scope),
                fontSize = 11.sp, color = TreemivoMuted)
            if (changes.isEmpty()) Text(stringResource(R.string.changed_files_none),
                fontSize = 12.sp, color = TreemivoMuted)
            changes.take(if (expanded) 25 else 5).forEach { item ->
                Surface(
                    color = TreemivoSurface, shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { preview = item }
                ) {
                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.file.name, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(
                                item.file.relativePath, fontSize = 11.sp,
                                color = TreemivoMuted, maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        val sizeDelta = item.deltaBytes
                        Text(
                            (if (sizeDelta > 0L) "+" else "") + deltaBytes(sizeDelta),
                            color = TreemivoAccent, fontSize = 12.sp
                        )
                    }
                }
            }
            if (changes.size > 5) TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(
                    if (expanded) R.string.changed_files_less else R.string.changed_files_more
                ))
            }
        }
    }
    preview?.let { item ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(item.file.name) },
            text = { FilePreview(item.file) },
            confirmButton = {
                TextButton(onClick = { preview = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

private fun deltaBytes(bytes: Long): String {
    var value = kotlin.math.abs(bytes.toDouble())
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return (if (bytes < 0) "-" else "") +
        String.format(Locale.getDefault(), "%.1f %s", value, units[index])
}
