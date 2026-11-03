package studio.tivaroid.treemivo.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import studio.tivaroid.treemivo.R
import studio.tivaroid.treemivo.data.FileRecord
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

data class ZipEntryInfo(val name: String, val originalBytes: Long, val compressedBytes: Long)

private suspend fun inspectZip(context: Context, file: FileRecord): List<ZipEntryInfo> =
    withContext(Dispatchers.IO) {
        // Do not unpack or inflate archive entries. Cap temporary compressed bytes.
        val maximum = 32L * 1024 * 1024
        if (file.sizeBytes > maximum) throw IOException("Archive exceeds 32 MB preview limit")
        val temp = File.createTempFile("treemivo_zip_", ".zip", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(file.uri)
                ?: throw IOException("No access to archive")
            input.use { source ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        written += count
                        if (written > maximum) throw IOException("Archive exceeds 32 MB preview limit")
                        output.write(buffer, 0, count)
                    }
                }
            }
            ZipFile(temp).use { archive ->
                val entries = mutableListOf<ZipEntryInfo>()
                val iterator = archive.entries()
                while (iterator.hasMoreElements() && entries.size < 150) {
                    currentCoroutineContext().ensureActive()
                    val entry = iterator.nextElement()
                    if (!entry.isDirectory) entries.add(
                        ZipEntryInfo(entry.name, entry.size, entry.compressedSize)
                    )
                }
                entries
            }
        } finally {
            temp.delete()
        }
    }

@Composable
fun ArchiveInspector(file: FileRecord) {
    val context = LocalContext.current
    var requested by remember(file.uri) { mutableStateOf(false) }
    val result by produceState<Result<List<ZipEntryInfo>>?>(null, requested, file.uri) {
        if (requested) value = runCatching { inspectZip(context, file) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { requested = true }, enabled = result == null) {
            Text(stringResource(R.string.inspect_zip))
        }
        val entries = result?.getOrNull()
        if (result?.isFailure == true) {
            Text(stringResource(R.string.zip_unavailable), color = TreemivoMuted)
        }
        if (entries != null) {
            Text(stringResource(R.string.zip_metadata_warning), color = TreemivoMuted)
            Text("${entries.size} " + stringResource(R.string.files_count).lowercase())
            entries.take(20).forEach { item ->
                Text(item.name + " · " +
                    if (item.originalBytes >= 0) "${item.originalBytes} B"
                    else "?", color = Color.White, maxLines = 1)
            }
        }
    }
}
