package studio.tivaroid.treemivo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedFilter(
    val name: String,
    val minSize: Long,
    val maxSize: Long,
    val kind: FileKind?,
    val recentOnly: Boolean,
    val sort: FileSort,
    val onlyEmpty: Boolean = false,
    val onlyHidden: Boolean = false,
    val extension: String? = null,
    val searchQuery: String = ""
)

/** Saved presets may include user-entered query text, stored privately on device. */
class FilterPresets(context: Context) {
    private val prefs = context.getSharedPreferences("treemivo_filters", Context.MODE_PRIVATE)

    fun load(): List<SavedFilter> = runCatching {
        val array = JSONArray(prefs.getString("presets", "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            SavedFilter(
                name = item.getString("name"),
                minSize = item.getLong("min"),
                maxSize = item.getLong("max"),
                kind = item.optString("kind").takeIf { it.isNotBlank() }
                    ?.let { FileKind.valueOf(it) },
                recentOnly = item.getBoolean("recent"),
                sort = FileSort.valueOf(item.getString("sort")),
                onlyEmpty = item.optBoolean("emptyOnly", false),
                onlyHidden = item.optBoolean("hiddenOnly", false),
                extension = item.optString("extension").takeIf { it.isNotBlank() },
                searchQuery = item.optString("searchQuery", "")
            )
        }
    }.getOrDefault(emptyList())

    fun save(preset: SavedFilter) {
        require(preset.name.isNotBlank() && preset.name.length <= 40)
        val entries = (load().filterNot { it.name == preset.name } + preset).takeLast(10)
        val arr = JSONArray()
        entries.forEach { item ->
            arr.put(JSONObject().apply {
                put("name", item.name)
                put("min", item.minSize)
                put("max", item.maxSize)
                put("kind", item.kind?.name ?: "")
                put("recent", item.recentOnly)
                put("sort", item.sort.name)
                put("emptyOnly", item.onlyEmpty)
                put("hiddenOnly", item.onlyHidden)
                put("extension", item.extension ?: "")
                put("searchQuery", item.searchQuery.take(300))
            })
        }
        prefs.edit().putString("presets", arr.toString()).apply()
    }

    fun remove(name: String) {
        val all = load().filterNot { it.name == name }
        val arr = JSONArray()
        all.forEach {
            arr.put(JSONObject().apply {
                put("name", it.name); put("min", it.minSize); put("max", it.maxSize)
                put("kind", it.kind?.name ?: ""); put("recent", it.recentOnly)
                put("sort", it.sort.name); put("emptyOnly", it.onlyEmpty)
                put("hiddenOnly", it.onlyHidden); put("extension", it.extension ?: "")
                put("searchQuery", it.searchQuery.take(300))
            })
        }
        prefs.edit().putString("presets", arr.toString()).apply()
    }
}
