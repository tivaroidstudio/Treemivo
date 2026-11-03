package studio.tivaroid.treemivo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.tivaroid.treemivo.R
import studio.tivaroid.treemivo.data.*
import java.util.Locale

@Composable
fun SmartCollectionsDialog(
    files: List<FileRecord>,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(CollectionMode.FOLDERS) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<FileRecord?>(null) }
    val collections by produceState(
        initialValue = emptyList<SmartCollection>(), files, mode
    ) {
        value = withContext(Dispatchers.Default) {
            makeCollections(files, mode)
        }
    }
    val selected = collections.firstOrNull { it.id == selectedId }
    LaunchedEffect(mode) { selectedId = null }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.96f).fillMaxHeight(.88f).widthIn(max = 980.dp),
            shape = RoundedCornerShape(22.dp),
            color = TreemivoPanel
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.collections_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
                Text(stringResource(R.string.collections_scope),
                    color = TreemivoMuted, fontSize = 12.sp)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CollectionMode.entries.forEach { choice ->
                        FilterChip(
                            selected = mode == choice,
                            onClick = { mode = choice },
                            label = { Text(stringResource(choice.labelResource())) }
                        )
                    }
                }
                if (selected != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { selectedId = null }) {
                            Text(stringResource(R.string.collections_back))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(selected.label, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    if (selected == null) {
                        items(collections, key = { it.id }) { collection ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { selectedId = collection.id },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = TreemivoSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(13.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Folder, null, tint = TreemivoAccent)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                collectionLabel(mode, collection.label),
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${collection.count} " +
                                                    stringResource(R.string.files_count).lowercase(),
                                                color = TreemivoMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(collectionSize(collection.bytes),
                                            color = TreemivoMuted, fontSize = 12.sp)
                                        Icon(Icons.Outlined.ChevronRight, null,
                                            tint = TreemivoMuted)
                                    }
                                }
                            }
                        }
                        if (collections.isEmpty()) item {
                            Text(stringResource(R.string.collections_empty),
                                color = TreemivoMuted)
                        }
                    } else {
                        items(selected.members.take(300), key = { it.uri.toString() }) { file ->
                            Surface(
                                color = TreemivoSurface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().clickable { preview = file }
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(file.name, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Text(file.relativePath, color = TreemivoMuted,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            fontSize = 11.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(collectionSize(file.sizeBytes),
                                        fontSize = 12.sp, color = TreemivoMuted)
                                }
                            }
                        }
                        if (selected.members.size > 300) item {
                            Text(stringResource(R.string.collections_preview_limit),
                                color = TreemivoMuted, fontSize = 12.sp)
                        }
                    }
                }
                Text(stringResource(R.string.collections_not_duplicates),
                    color = TreemivoMuted, fontSize = 11.sp)
            }
        }
    }
    preview?.let { file ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(file.name, maxLines = 2) },
            text = { FilePreview(file) },
            confirmButton = {
                TextButton(onClick = { preview = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

private fun CollectionMode.labelResource(): Int = when (this) {
    CollectionMode.FOLDERS -> R.string.collections_folders
    CollectionMode.EXTENSIONS -> R.string.collections_extensions
    CollectionMode.YEARS -> R.string.collections_years
    CollectionMode.NAME_FAMILIES -> R.string.collections_name_families
    CollectionMode.FILE_SIZES -> R.string.collections_file_sizes
}

@Composable
private fun collectionLabel(mode: CollectionMode, label: String): String {
    if (mode != CollectionMode.FILE_SIZES) return when (label) {
        "(root)" -> stringResource(R.string.folder_root)
        "(none)" -> stringResource(R.string.insights_no_extension)
        "(unknown)" -> stringResource(R.string.insights_date_unknown)
        else -> label
    }
    return stringResource(when (label) {
        "unknown" -> R.string.collections_unknown_size
        "empty" -> R.string.collections_empty_size
        "under_10mb" -> R.string.collections_under_10mb
        "10_100mb" -> R.string.collections_10_100mb
        "100mb_1gb" -> R.string.collections_100mb_1gb
        else -> R.string.collections_over_1gb
    })
}

private fun collectionSize(bytes: Long): String {
    var value = bytes.coerceAtLeast(0).toDouble()
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
}
