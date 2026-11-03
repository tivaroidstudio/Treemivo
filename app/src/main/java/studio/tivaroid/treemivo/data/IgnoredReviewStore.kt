package studio.tivaroid.treemivo.data

import android.content.Context
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Keeps a small local allow-list of items users do not want suggested for cleanup.
 * IDs are keyed HMACs of Content URIs; no filenames, paths or file data are persisted.
 */
class IgnoredReviewStore(context: Context) {
    private val prefs = context.getSharedPreferences(
        "treemivo_review_ignored", Context.MODE_PRIVATE
    )
    private val secret: String by lazy {
        prefs.getString("key", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("key", it).apply()
        }
    }

    fun token(file: FileRecord): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val bytes = mac.doFinal(file.uri.toString().toByteArray(Charsets.UTF_8))
        return bytes.take(16).joinToString("") { "%02x".format(it) }
    }

    fun ignoredTokens(): Set<String> =
        prefs.getStringSet("ignored", emptySet()).orEmpty().toSet()

    fun ignore(file: FileRecord) {
        val all = ignoredTokens()
        val target = token(file)
        if (target in all) return
        val next = (all + target).takeLast(2000).toSet()
        prefs.edit().putStringSet("ignored", next).apply()
    }

    fun restoreAll() {
        prefs.edit().remove("ignored").apply()
    }
}
