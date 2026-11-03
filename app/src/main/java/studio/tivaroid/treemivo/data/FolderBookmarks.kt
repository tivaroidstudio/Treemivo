package studio.tivaroid.treemivo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class FolderBookmark(
    val source: ScanOrigin,
    val path: String
) {
    val title: String get() = path.substringAfterLast('/').ifBlank { path }
}

/**
 * Quick links to directories the user has already indexed.
 * No additional storage grants are requested. Bookmarks are not a scan source.
 */
class FolderBookmarks(context: Context) {
    private val preferences = context.getSharedPreferences(
        "treemivo_folder_bookmarks", Context.MODE_PRIVATE
    )

    fun load(): List<FolderBookmark> = runCatching {
        val saved = JSONArray(preferences.getString("folders", "[]"))
        (0 until saved.length()).mapNotNull { i ->
            val item = saved.getJSONObject(i)
            val source = runCatching {
                ScanOrigin.valueOf(item.getString("source"))
            }.getOrNull() ?: return@mapNotNull null
            val path = item.getString("path")
            if (path.isBlank()) null else FolderBookmark(source, path)
        }.distinct().take(24)
    }.getOrDefault(emptyList())

    fun save(list: List<FolderBookmark>) {
        val entries = list.filter { it.path.isNotBlank() }
            .distinct().takeLast(24)
        val data = JSONArray()
        for (item in entries) {
            data.put(JSONObject().apply {
                put("source", item.source.name)
                put("path", item.path)
            })
        }
        preferences.edit().putString("folders", data.toString()).apply()
    }
}
