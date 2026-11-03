package studio.tivaroid.treemivo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** All metadata stays on-device. Folder keys are HMACs, not readable path names. */
data class ScanSnapshot(
    val time: Long,
    val source: String,
    val count: Int,
    val bytes: Long,
    val largest: Long,
    val folderSizes: Map<String, Long> = emptyMap(),
    val scopeKey: String = "",
    val partial: Boolean = true,
    val typeBytes: Map<String, Long> = emptyMap(),
    val trackedFiles: Map<String, Long> = emptyMap()
)

data class FolderChange(
    val currentPath: String,
    val previousBytes: Long,
    val currentBytes: Long
) {
    val delta: Long get() = currentBytes - previousBytes
}

data class TrackedFileChange(
    val file: FileRecord,
    val beforeBytes: Long
) {
    val deltaBytes: Long get() = file.sizeBytes - beforeBytes
}

class ScanHistory(context: Context) {
    private val prefs = context.getSharedPreferences("treemivo_history", Context.MODE_PRIVATE)
    private val salt: String by lazy {
        prefs.getString("folder_key", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("folder_key", it).apply()
        }
    }

    private fun digest(path: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(path.toByteArray(Charsets.UTF_8))
            .take(16).joinToString("") { "%02x".format(it) }
    }

    /**
     * HMAC-based fingerprint of the authorized sources (never exposes raw folder URIs).
     * A different grant set produces a different fingerprint, preventing false trends.
     */
    fun scopeKey(source: String, grants: List<String>): String =
        digest("source=" + source + "|grants=" + grants.sorted().joinToString("|"))

    fun entries(): List<ScanSnapshot> = try {
        val array = JSONArray(prefs.getString("entries", "[]"))
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            val folderData = item.optJSONObject("folders")
            val keys = folderData?.keys()?.asSequence()?.toList().orEmpty()
            val kinds = item.optJSONObject("types")
            val kindNames = kinds?.keys()?.asSequence()?.toList().orEmpty()
            val tracked = item.optJSONObject("tracked")
            val trackedKeys = tracked?.keys()?.asSequence()?.toList().orEmpty()
            ScanSnapshot(
                time = item.getLong("time"),
                source = item.getString("source"),
                count = item.getInt("count"),
                bytes = item.getLong("bytes"),
                largest = item.getLong("largest"),
                folderSizes = keys.associateWith { folderData!!.getLong(it) },
                scopeKey = item.optString("scope", ""),
                partial = item.optBoolean("partial", true),
                typeBytes = kindNames.associateWith { kinds!!.optLong(it, 0L) },
                trackedFiles = trackedKeys.associateWith { tracked!!.optLong(it, 0L) }
            )
        }
    } catch (_: Exception) { emptyList() }

    @Synchronized
    fun record(source: String, report: StorageReport, grants: List<String>) {
        val breakdown = FileKind.entries.associate { kind ->
            kind.name to report.files.asSequence().filter { it.kind == kind }
                .sumOf { it.sizeBytes }
        }
        val summary = ScanSnapshot(
            System.currentTimeMillis(), source, report.files.size,
            report.totalBytes, report.files.maxOfOrNull { it.sizeBytes } ?: 0L,
            report.folders.sortedByDescending { it.sizeBytes }.take(50)
                .associate { digest(it.relativePath) to it.sizeBytes },
            scopeKey = scopeKey(source, grants),
            partial = report.partial,
            typeBytes = breakdown,
            trackedFiles = report.files.asSequence()
                .filter { it.sizeKnown && it.sizeBytes > 0L }
                .sortedByDescending { it.sizeBytes }
                .distinctBy { it.uri.toString() }
                .take(200)
                .associate { digest(it.uri.toString()) to it.sizeBytes }
        )
        val array = JSONArray()
        (listOf(summary) + entries()).take(30).forEach { snapshot ->
            val folders = JSONObject()
            snapshot.folderSizes.forEach { (key, size) -> folders.put(key, size) }
            val types = JSONObject()
            snapshot.typeBytes.forEach { (key, bytes) -> types.put(key, bytes) }
            val tracked = JSONObject()
            snapshot.trackedFiles.forEach { (key, bytes) -> tracked.put(key, bytes) }
            array.put(JSONObject().apply {
                put("time", snapshot.time)
                put("source", snapshot.source)
                put("count", snapshot.count)
                put("bytes", snapshot.bytes)
                put("largest", snapshot.largest)
                put("folders", folders)
                put("types", types)
                put("tracked", tracked)
                put("scope", snapshot.scopeKey)
                put("partial", snapshot.partial)
            })
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }

    /**
     * Shows only currently accessible top folders. Missing/renamed folders
     * are not attributed to an arbitrary path.
     */
    fun compare(
        snapshot: ScanSnapshot,
        current: StorageReport,
        currentScopeKey: String
    ): List<FolderChange> {
        if (snapshot.partial || current.partial ||
            snapshot.scopeKey.isBlank() || snapshot.scopeKey != currentScopeKey
        ) return emptyList()
        return current.folders.take(50).mapNotNull { folder ->
            val old = snapshot.folderSizes[digest(folder.relativePath)] ?: return@mapNotNull null
            FolderChange(folder.relativePath, old, folder.sizeBytes)
        }.filter { it.delta != 0L }
            .sortedByDescending { kotlin.math.abs(it.delta) }
    }

    /**
     * Compare only the same tracked file URI in a matching, complete scope.
     * Top-200 samples may change; files that weren't previously tracked are not called new.
     */
    fun changedTrackedFiles(
        previous: ScanSnapshot,
        current: StorageReport,
        currentScopeKey: String
    ): List<TrackedFileChange> {
        if (previous.partial || current.partial ||
            previous.scopeKey.isBlank() ||
            previous.scopeKey != currentScopeKey ||
            previous.trackedFiles.isEmpty()
        ) return emptyList()
        return current.files.asSequence()
            .filter { it.sizeKnown && it.sizeBytes > 0L }
            .sortedByDescending { it.sizeBytes }
            .distinctBy { it.uri.toString() }
            .take(200)
            .mapNotNull { file ->
                val before = previous.trackedFiles[digest(file.uri.toString())]
                    ?: return@mapNotNull null
                if (before == file.sizeBytes) null else TrackedFileChange(file, before)
            }
            .sortedByDescending { kotlin.math.abs(it.deltaBytes) }
            .take(25)
            .toList()
    }

    fun clear() = prefs.edit().remove("entries").apply()
}
