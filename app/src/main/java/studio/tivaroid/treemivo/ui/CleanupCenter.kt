package studio.tivaroid.treemivo.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.tivaroid.treemivo.R
import studio.tivaroid.treemivo.data.*

private enum class CleanupCategory {
    LARGE_VIDEO, OLD_DOWNLOAD, INSTALLERS, DUPLICATES, SIMILAR
}

/** The app cannot silently remove documents. Only selected MediaStore URIs get a batch trash request. */
@Composable
fun CleanupCenter(vm: StorageViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val report = vm.report
    var category by remember { mutableStateOf(CleanupCategory.LARGE_VIDEO) }
    val checked = remember { mutableStateListOf<String>() }
    var confirmBatch by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<FileRecord?>(null) }
    val trash = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        checked.clear()
        if (result.resultCode == Activity.RESULT_OK) vm.refresh()
    }
    val current = report?.files.orEmpty()
    val now = remember { System.currentTimeMillis() }
    val suggestions by produceState(
        initialValue = emptyList<FileRecord>(), report, category, vm.duplicates, vm.similarGroups
    ) {
        value = withContext(Dispatchers.Default) {
            when (category) {
                CleanupCategory.LARGE_VIDEO ->
                    current.filter { it.kind == FileKind.VIDEO && it.sizeBytes >= 100L * 1024 * 1024 }
                CleanupCategory.OLD_DOWNLOAD ->
                    current.filter {
                        (it.relativePath.contains("download", ignoreCase = true)) &&
                        it.modifiedAtMillis > 0 &&
                        it.modifiedAtMillis < now - 30L * 24 * 60 * 60 * 1000
                    }
                CleanupCategory.INSTALLERS ->
                    current.filter { it.kind == FileKind.INSTALLER }
                CleanupCategory.DUPLICATES ->
                    vm.duplicates.flatMap { it.files.drop(1) }
                CleanupCategory.SIMILAR ->
                    vm.similarGroups.flatMap { it.files }
            }.distinctBy { it.uri.toString() }.sortedByDescending { it.sizeBytes }
        }
    }
    val chosen = remember(checked.toList(), suggestions) {
        val keys = checked.toSet()
        suggestions.filter { it.uri.toString() in keys && SafeFileRemoval.supportsTrash(it) }
            .distinctBy { it.uri.toString() }
            .take(500)
    }
    val suggestedTotal = suggestions.sumOf { it.sizeBytes }
    val canUse = vm.billing.isPro || category in listOf(
        CleanupCategory.LARGE_VIDEO, CleanupCategory.INSTALLERS, CleanupCategory.DUPLICATES
    )

    LaunchedEffect(category) { checked.clear() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            Modifier.fillMaxWidth(.96f).fillMaxHeight(.90f).widthIn(max = 950.dp),
            shape = RoundedCornerShape(22.dp),
            color = TreemivoPanel
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cleanup_title), style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                }
                Text(stringResource(R.string.cleanup_explanation), color = TreemivoMuted, fontSize = 12.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CleanupCategory.entries.forEach { option ->
                        val label = when (option) {
                            CleanupCategory.LARGE_VIDEO -> R.string.cleanup_big_videos
                            CleanupCategory.OLD_DOWNLOAD -> R.string.cleanup_old_downloads
                            CleanupCategory.INSTALLERS -> R.string.cleanup_installers
                            CleanupCategory.DUPLICATES -> R.string.duplicates
                            CleanupCategory.SIMILAR -> R.string.similar_photos
                        }
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(stringResource(label)) }
                        )
                    }
                }
                if (!canUse) {
                    Text(stringResource(R.string.cleanup_pro_only), color = TreemivoMuted)
                    val price = vm.billing.price
                    Button(
                        enabled = price != null,
                        onClick = { (context as? Activity)?.let { vm.billing.purchase(it) } }
                    ) { Text(stringResource(R.string.buy_pro) + (price?.let { " · $it" } ?: "")) }
                } else {
                    if (category == CleanupCategory.DUPLICATES && !vm.duplicatesChecked) {
                        Button(onClick = vm::verifyDuplicates, enabled = !vm.checkingDuplicates) {
                            Text(stringResource(R.string.find_duplicates))
                        }
                    }
                    if (category == CleanupCategory.SIMILAR) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = vm::analyzeSimilarPhotos,
                                enabled = !vm.similarBusy && vm.billing.isPro
                            ) { Text(stringResource(R.string.analyze_similar)) }
                            if (vm.similarBusy) {
                                Spacer(Modifier.width(8.dp))
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(" ${vm.similarProgress}", fontSize = 12.sp)
                                TextButton(onClick = vm::cancelSimilarPhotos) {
                                    Text(stringResource(R.string.cancel_scan))
                                }
                            }
                        }
                        Text(stringResource(R.string.similar_disclaimer),
                            color = TreemivoMuted, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${suggestions.size} " + stringResource(R.string.files_count).lowercase())
                            Text("${formatSpace(suggestedTotal)} " +
                                stringResource(R.string.cleanup_review_only),
                                color = TreemivoMuted, fontSize = 12.sp)
                        }
                        if (chosen.isNotEmpty()) {
                            TextButton(onClick = { checked.clear() }) {
                                Text(stringResource(R.string.clear_selection))
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(suggestions.take(300), key = { it.uri.toString() }) { file ->
                            val key = file.uri.toString()
                            val canTrash = Build.VERSION.SDK_INT >= 30 &&
                                SafeFileRemoval.supportsTrash(file)
                            Surface(shape = RoundedCornerShape(12.dp), color = TreemivoSurface) {
                                Row(Modifier.fillMaxWidth().padding(9.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = key in checked,
                                        enabled = canTrash,
                                        onCheckedChange = { selected ->
                                            if (selected && key !in checked) checked.add(key)
                                            else if (!selected) checked.remove(key)
                                        }
                                    )
                                    Column(Modifier.weight(1f).clickable { previewFile = file }) {
                                        Text(file.name, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Text(file.relativePath, color = TreemivoMuted,
                                            fontSize = 11.sp, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(formatSpace(file.sizeBytes), fontSize = 12.sp)
                                }
                            }
                        }
                        if (suggestions.isEmpty()) item {
                            Text(stringResource(R.string.cleanup_none), color = TreemivoMuted)
                        }
                    }
                    Text(stringResource(R.string.cleanup_trash_scope), color = TreemivoMuted, fontSize = 11.sp)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { confirmBatch = true },
                        enabled = chosen.isNotEmpty() && Build.VERSION.SDK_INT >= 30
                    ) {
                        Text(stringResource(R.string.move_selected_to_trash) +
                            " · ${chosen.size} · ${formatSpace(chosen.sumOf { it.sizeBytes })}")
                    }
                }
            }
        }
    }
    if (confirmBatch) AlertDialog(
        onDismissRequest = { confirmBatch = false },
        title = { Text(stringResource(R.string.confirm_remove_title)) },
        text = {
            Text(stringResource(R.string.cleanup_confirm_batch) +
                " ${chosen.size} · ${formatSpace(chosen.sumOf { it.sizeBytes })}")
        },
        dismissButton = {
            TextButton(onClick = { confirmBatch = false }) {
                Text(stringResource(R.string.cancel_scan))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                confirmBatch = false
                if (chosen.isNotEmpty() && Build.VERSION.SDK_INT >= 30) {
                    runCatching {
                        trash.launch(IntentSenderRequest.Builder(
                            SafeFileRemoval.trashBatchRequest(context, chosen)
                        ).build())
                    }
                }
            }) { Text(stringResource(R.string.move_to_trash)) }
        }
    )
    previewFile?.let { file ->
        AlertDialog(
            onDismissRequest = { previewFile = null },
            title = { Text(file.name, maxLines = 2) },
            text = { FilePreview(file) },
            confirmButton = {
                TextButton(onClick = { previewFile = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

private fun formatSpace(bytes: Long): String {
    var amount = bytes.toDouble()
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var unit = 0
    while (amount >= 1024.0 && unit < units.lastIndex) { amount /= 1024.0; unit++ }
    return "%.1f %s".format(java.util.Locale.getDefault(), amount, units[unit])
}
