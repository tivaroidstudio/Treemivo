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
    val folderSizes: Map<String, Long> = emptyMap()
)

data class FolderChange(
    val currentPath: String,
    val previousBytes: Long,
    val currentBytes: Long
) {
    val delta: Long get() = currentBytes - previousBytes
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

    fun entries(): List<ScanSnapshot> = try {
        val array = JSONArray(prefs.getString("entries", "[]"))
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            val folderData = item.optJSONObject("folders")
            val keys = folderData?.keys()?.asSequence()?.toList().orEmpty()
            ScanSnapshot(
                time = item.getLong("time"),
                source = item.getString("source"),
                count = item.getInt("count"),
                bytes = item.getLong("bytes"),
                largest = item.getLong("largest"),
                folderSizes = keys.associateWith { folderData!!.getLong(it) }
            )
        }
    } catch (_: Exception) { emptyList() }

    @Synchronized
    fun record(source: String, report: StorageReport) {
        val summary = ScanSnapshot(
            System.currentTimeMillis(), source, report.files.size,
            report.totalBytes, report.files.maxOfOrNull { it.sizeBytes } ?: 0L,
            report.folders.sortedByDescending { it.sizeBytes }.take(50)
                .associate { digest(it.relativePath) to it.sizeBytes }
        )
        val array = JSONArray()
        (listOf(summary) + entries()).take(30).forEach { snapshot ->
            val folders = JSONObject()
            snapshot.folderSizes.forEach { (key, size) -> folders.put(key, size) }
            array.put(JSONObject().apply {
                put("time", snapshot.time)
                put("source", snapshot.source)
                put("count", snapshot.count)
                put("bytes", snapshot.bytes)
                put("largest", snapshot.largest)
                put("folders", folders)
            })
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }

    /**
     * Shows only currently accessible top folders. Missing/renamed folders
     * are not attributed to an arbitrary path.
     */
    fun compare(snapshot: ScanSnapshot, current: StorageReport): List<FolderChange> {
        return current.folders.take(50).mapNotNull { folder ->
            val old = snapshot.folderSizes[digest(folder.relativePath)] ?: return@mapNotNull null
            FolderChange(folder.relativePath, old, folder.sizeBytes)
        }.filter { it.delta != 0L }
            .sortedByDescending { kotlin.math.abs(it.delta) }
    }

    fun clear() = prefs.edit().remove("entries").apply()
}
