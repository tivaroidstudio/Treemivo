package studio.tivaroid.treemivo.data

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeviceSpace(val usedBytes: Long, val totalBytes: Long)

class StorageViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<Application>()
    private val settings = app.getSharedPreferences("treemivo", 0)
    private val scanner = StorageScanner(app.contentResolver)

    var selectedTree by mutableStateOf(settings.getString("tree_uri", null)?.let(Uri::parse))
        private set
    var report by mutableStateOf<StorageReport?>(null)
        private set
    var duplicates by mutableStateOf<List<DuplicateGroup>>(emptyList())
        private set
    var duplicatesChecked by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var checkingDuplicates by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val deviceSpace: DeviceSpace
        get() = try {
            @Suppress("DEPRECATION")
            val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
            val total = stat.totalBytes
            DeviceSpace((total - stat.availableBytes).coerceAtLeast(0), total)
        } catch (_: Exception) {
            DeviceSpace(0, 0)
        }

    init { if (selectedTree != null) refresh() }

    fun setTree(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            errorMessage = e.localizedMessage
            return
        }
        selectedTree = uri
        settings.edit().putString("tree_uri", uri.toString()).apply()
        refresh()
    }

    fun refresh() {
        val uri = selectedTree ?: return
        if (busy) return
        busy = true
        errorMessage = null
        duplicates = emptyList()
        duplicatesChecked = false
        viewModelScope.launch {
            try {
                report = withContext(Dispatchers.IO) { scanner.scan(uri) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report = null
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            } finally {
                busy = false
            }
        }
    }

    fun verifyDuplicates() {
        val snapshot = report ?: return
        if (checkingDuplicates) return
        checkingDuplicates = true
        viewModelScope.launch {
            try {
                duplicates = withContext(Dispatchers.IO) {
                    scanner.findExactDuplicates(snapshot.files)
                }
                duplicatesChecked = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            } finally {
                checkingDuplicates = false
            }
        }
    }

    fun exportCsv(uri: Uri) {
        val snapshot = report ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                    writer.appendLine("path,name,size_bytes,mime_type")
                    for (file in snapshot.files) {
                        writer.appendLine(listOf(
                            file.relativePath, file.name,
                            file.sizeBytes.toString(), file.mimeType
                        ).joinToString(",") { "\""+it.replace("\"", "\"\"")+"\"" })
                    }
                } ?: error("Cannot open export destination")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.localizedMessage ?: e.javaClass.simpleName
                }
            }
        }
    }

    fun clearError() { errorMessage = null }
}
