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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import studio.tivaroid.treemivo.billing.ProBilling

data class DeviceSpace(val usedBytes: Long, val totalBytes: Long)
enum class ScanOrigin { FOLDERS, MEDIA }

class StorageViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<Application>()
    private val prefs = app.getSharedPreferences("treemivo", 0)
    private val safScanner = StorageScanner(app.contentResolver)
    private val mediaScanner = MediaScanner(app)
    private val historyStore = ScanHistory(app)
    private val filterStore = FilterPresets(app)
    private val photoAnalyzer = PhotoSimilarityAnalyzer(app.contentResolver)
    val billing = ProBilling(app)
    private var scanJob: Job? = null
    private var duplicateJob: Job? = null
    private var photoJob: Job? = null
    private var scanGeneration: Long = 0L

    var treeUris by mutableStateOf(loadFolderUris())
        private set
    val selectedTree: Uri? get() = treeUris.lastOrNull()
    var origin by mutableStateOf(
        runCatching {
            ScanOrigin.valueOf(prefs.getString("origin", "FOLDERS") ?: "FOLDERS")
        }.getOrDefault(ScanOrigin.FOLDERS)
    )
        private set
    var report by mutableStateOf<StorageReport?>(null)
        private set
    var history by mutableStateOf(historyStore.entries())
        private set
    var savedFilters by mutableStateOf(filterStore.load())
        private set
    var similarGroups by mutableStateOf<List<SimilarPhotoGroup>>(emptyList())
        private set
    var similarBusy by mutableStateOf(false)
        private set
    var similarProgress by mutableStateOf(0)
        private set
    var duplicates by mutableStateOf<List<DuplicateGroup>>(emptyList())
        private set
    var duplicatesChecked by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var progressFiles by mutableStateOf(0)
        private set
    var checkingDuplicates by mutableStateOf(false)
        private set
    var duplicatesProgress by mutableStateOf(0)
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

    init {
        if (origin == ScanOrigin.FOLDERS && treeUris.isNotEmpty()) refresh()
        if (origin == ScanOrigin.MEDIA && mediaScanner.hasAccess()) refresh()
    }

    private fun loadFolderUris(): List<Uri> {
        return try {
            val raw = prefs.getString("trees", null)
            if (raw != null) {
                val array = JSONArray(raw)
                (0 until array.length()).map { Uri.parse(array.getString(it)) }.distinct()
            } else {
                // Migration from initial single-folder prototype.
                prefs.getString("tree_uri", null)?.let { listOf(Uri.parse(it)) } ?: emptyList()
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun persistFolders() {
        val json = JSONArray()
        treeUris.forEach { json.put(it.toString()) }
        prefs.edit().putString("trees", json.toString()).apply()
    }

    fun hasMediaAccess(): Boolean = mediaScanner.hasAccess()

    fun setTree(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // The provider might not offer a persistable write grant.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        } catch (e: SecurityException) {
            errorMessage = e.localizedMessage
            return
        }
        if (uri !in treeUris) treeUris = treeUris + uri
        persistFolders()
        selectOrigin(ScanOrigin.FOLDERS)
    }

    fun removeTree(uri: Uri) {
        treeUris = treeUris - uri
        persistFolders()
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        if (origin == ScanOrigin.FOLDERS) {
            if (treeUris.isEmpty()) {
                scanJob?.cancel()
                report = null
            } else refresh()
        }
    }

    fun selectOrigin(value: ScanOrigin) {
        origin = value
        prefs.edit().putString("origin", value.name).apply()
        scanJob?.cancel()
        report = null
        if (value == ScanOrigin.MEDIA && !hasMediaAccess()) {
            errorMessage = "Media permission is needed to scan shared photos, videos and audio."
            return
        }
        if (value == ScanOrigin.FOLDERS && treeUris.isEmpty()) return
        refresh()
    }

    fun refresh() {
        if (origin == ScanOrigin.FOLDERS && treeUris.isEmpty()) return
        if (origin == ScanOrigin.MEDIA && !hasMediaAccess()) return
        scanJob?.cancel()
        duplicateJob?.cancel()
        photoJob?.cancel()
        similarGroups = emptyList()
        similarBusy = false
        scanGeneration++
        val generation = scanGeneration
        duplicates = emptyList()
        duplicatesChecked = false
        checkingDuplicates = false
        progressFiles = 0
        val currentMode = origin
        val currentTrees = treeUris.toList()
        scanJob = viewModelScope.launch {
            busy = true
            errorMessage = null
            try {
                val fresh = withContext(Dispatchers.IO) {
                    if (currentMode == ScanOrigin.MEDIA) {
                        mediaScanner.scan { if (generation == scanGeneration) progressFiles = it }
                    } else {
                        var completedCount = 0
                        val parts = currentTrees.map { uri ->
                            val offset = completedCount
                            val part = safScanner.scan(uri) {
                                if (generation == scanGeneration) progressFiles = offset + it
                            }
                            completedCount += part.files.size + part.folders.size
                            part
                        }
                        if (parts.size == 1) {
                            parts.single()
                        } else {
                            // Prefix each SAF tree to avoid folder-name collisions.
                            val merged = parts.flatMapIndexed { index, part ->
                                val prefix = (index + 1).toString() + " - " + part.rootName
                                part.files.map { file ->
                                    file.copy(relativePath = "$prefix/${file.relativePath}")
                                }
                            }.distinctBy { it.uri.toString() }
                            StorageReport(
                                rootName = "Selected folders (${parts.size})",
                                files = merged,
                                folders = folderBreakdown(merged),
                                partial = parts.any { it.partial }
                            )
                        }
                    }
                }
                report = fresh
                withContext(Dispatchers.IO) {
                    historyStore.record(currentMode.name, fresh)
                }
                history = historyStore.entries()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            } finally {
                if (generation == scanGeneration) busy = false
            }
        }
    }

    fun cancelScan() {
        scanGeneration++
        scanJob?.cancel()
        busy = false
    }

    fun saveFilter(preset: SavedFilter) {
        if (!billing.isPro) return
        filterStore.save(preset)
        savedFilters = filterStore.load()
    }

    fun removeFilter(name: String) {
        if (!billing.isPro) return
        filterStore.remove(name)
        savedFilters = filterStore.load()
    }

    fun analyzeSimilarPhotos() {
        if (!billing.isPro || similarBusy) return
        val snapshot = report ?: return
        similarBusy = true
        similarProgress = 0
        photoJob = viewModelScope.launch {
            try {
                similarGroups = withContext(Dispatchers.IO) {
                    photoAnalyzer.analyze(snapshot.files) { done, _ ->
                        similarProgress = done
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            } finally {
                similarBusy = false
            }
        }
    }

    fun cancelSimilarPhotos() {
        photoJob?.cancel()
        similarBusy = false
    }

    fun exportPdf(destination: Uri) {
        if (!billing.isPro) return
        val snapshot = report ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                StoragePdfReport.write(context, destination, snapshot)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.localizedMessage ?: e.javaClass.simpleName
                }
            }
        }
    }

    fun folderChanges(snapshot: ScanSnapshot): List<FolderChange> {
        val current = report ?: return emptyList()
        if (snapshot.source != origin.name) return emptyList()
        return historyStore.compare(snapshot, current)
    }

    fun clearHistory() {
        historyStore.clear()
        history = emptyList()
    }

    fun verifyDuplicates() {
        val snapshot = report ?: return
        if (checkingDuplicates || busy) return
        checkingDuplicates = true
        duplicatesProgress = 0
        duplicateJob = viewModelScope.launch {
            try {
                duplicates = withContext(Dispatchers.IO) {
                    safScanner.findExactDuplicates(snapshot.files) { duplicatesProgress = it }
                }
                duplicatesChecked = true
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            } finally {
                checkingDuplicates = false
            }
        }
    }

    fun cancelDuplicateCheck() {
        duplicateJob?.cancel()
        checkingDuplicates = false
        duplicatesProgress = 0
    }

    /** SAF deletion is intentionally irreversible: this method must follow a confirmation dialog. */
    fun deleteSaf(file: FileRecord) {
        if (file.source != FileSource.SAF) return
        viewModelScope.launch {
            try {
                val removed = withContext(Dispatchers.IO) {
                    SafeFileRemoval.deleteSaf(context.contentResolver, file)
                }
                if (removed) refresh()
                else errorMessage = "The document provider refused deletion."
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            }
        }
    }

    fun exportCsv(uri: Uri) {
        val snapshot = report ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                    writer.appendLine("path,name,size_bytes,mime_type,modified_at_millis,source")
                    for (file in snapshot.files) {
                        writer.appendLine(listOf(
                            file.relativePath, file.name, file.sizeBytes.toString(),
                            file.mimeType, file.modifiedAtMillis.toString(), file.source.name
                        ).joinToString(",") { csvCell(it) })
                    }
                } ?: error("Cannot open export destination")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.localizedMessage ?: e.javaClass.simpleName
                }
            }
        }
    }

    private fun csvCell(raw: String): String {
        // Prevent spreadsheet formula injection when exported file names are opened in Excel.
        val safe = if (raw.trimStart().firstOrNull() in listOf('=', '+', '-', '@'))
            "'" + raw else raw
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    fun clearError() { errorMessage = null }

    override fun onCleared() {
        scanJob?.cancel()
        duplicateJob?.cancel()
        photoJob?.cancel()
        billing.close()
        super.onCleared()
    }
}
