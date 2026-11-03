package studio.tivaroid.treemivo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Aggregate-only history (no filenames, paths, tokens, or media bytes). */
data class ScanSnapshot(
    val time: Long, val source: String, val count: Int, val bytes: Long, val largest: Long
)

class ScanHistory(context: Context) {
    private val prefs = context.getSharedPreferences("treemivo_history", Context.MODE_PRIVATE)

    fun entries(): List<ScanSnapshot> = try {
        val arr = JSONArray(prefs.getString("entries", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ScanSnapshot(o.getLong("time"), o.getString("source"), o.getInt("count"),
                o.getLong("bytes"), o.getLong("largest"))
        }
    } catch (_: Exception) { emptyList() }

    @Synchronized
    fun record(source: String, report: StorageReport) {
        val row = ScanSnapshot(System.currentTimeMillis(), source, report.files.size,
            report.totalBytes, report.files.maxOfOrNull { it.sizeBytes } ?: 0L)
        val array = JSONArray()
        (listOf(row) + entries()).take(30).forEach {
            array.put(JSONObject().apply {
                put("time", it.time)
                put("source", it.source)
                put("count", it.count)
                put("bytes", it.bytes)
                put("largest", it.largest)
            })
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }
    fun clear() = prefs.edit().remove("entries").apply()
}
