package studio.tivaroid.treemivo.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale

/** A parsed, reusable search plan. Search only matches already-indexed accessible files. */
data class SearchPlan(
    val rules: List<SearchRule>,
    val errors: List<String>
) {
    fun matches(file: FileRecord): Boolean = rules.all { rule ->
        val result = rule.matches(file)
        if (rule.excluded) !result else result
    }
}

data class SearchRule(
    val excluded: Boolean,
    val matcher: SearchMatcher
) {
    fun matches(file: FileRecord): Boolean = matcher.matches(file)
}

/** Package-private to make query handling independent of Android UI dependencies. */
sealed interface SearchMatcher {
    fun matches(file: FileRecord): Boolean

    data class Text(val term: String) : SearchMatcher {
        override fun matches(file: FileRecord) =
            file.name.contains(term, ignoreCase = true) ||
                file.relativePath.contains(term, ignoreCase = true)
    }
    data class Name(val term: String) : SearchMatcher {
        override fun matches(file: FileRecord) = file.name.contains(term, ignoreCase = true)
    }
    data class Path(val term: String) : SearchMatcher {
        override fun matches(file: FileRecord) =
            file.relativePath.contains(term, ignoreCase = true)
    }
    data class Extension(val extensions: Set<String>) : SearchMatcher {
        override fun matches(file: FileRecord): Boolean {
            val ext = file.name.substringAfterLast('.', "")
                .lowercase(Locale.ROOT)
            return ext in extensions
        }
    }
    data class Kind(val kind: FileKind) : SearchMatcher {
        override fun matches(file: FileRecord) = file.kind == kind
    }
    data class Size(val bytes: Long, val greater: Boolean) : SearchMatcher {
        override fun matches(file: FileRecord) =
            file.sizeKnown && if (greater) file.sizeBytes >= bytes else file.sizeBytes <= bytes
    }
    data class DateRange(val millis: Long, val after: Boolean) : SearchMatcher {
        override fun matches(file: FileRecord) =
            file.modifiedAtMillis > 0L &&
                if (after) file.modifiedAtMillis >= millis else file.modifiedAtMillis < millis
    }
}

private val sizeUnits = mapOf(
    "B" to 1.0,
    "KB" to 1024.0,
    "MB" to 1024.0 * 1024,
    "GB" to 1024.0 * 1024 * 1024,
    "TB" to 1024.0 * 1024 * 1024 * 1024
)
private val sizeRegex = Regex("^([0-9]+(?:\\.[0-9]+)?)(B|KB|MB|GB|TB)?$", RegexOption.IGNORE_CASE)

private fun parseSize(value: String): Long? {
    val match = sizeRegex.matchEntire(value.trim()) ?: return null
    val number = match.groupValues[1].toDoubleOrNull() ?: return null
    val factor = sizeUnits[match.groupValues[2].uppercase(Locale.ROOT).ifBlank { "B" }]
        ?: return null
    val result = number * factor
    return if (result.isFinite() && result >= 0 && result <= Long.MAX_VALUE.toDouble())
        result.toLong() else null
}

private fun tokenizeSearch(query: String): Pair<List<String>, Boolean> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var quote: Char? = null
    var escape = false
    for (c in query) {
        if (escape) {
            current.append(c)
            escape = false
            continue
        }
        if (c == '\\' && quote != null) {
            escape = true
            continue
        }
        if (c == '"' || c == '\'') {
            if (quote == c) quote = null
            else if (quote == null) quote = c
            else current.append(c)
        } else if (c.isWhitespace() && quote == null) {
            if (current.isNotEmpty()) {
                tokens.add(current.toString())
                current.clear()
            }
        } else current.append(c)
    }
    if (current.isNotEmpty()) tokens.add(current.toString())
    return tokens to (quote != null)
}

/**
 * Query examples:
 *   type:video size>100MB
 *   ext:zip,7z path:Download
 *   name:"summer holiday" -ext:tmp
 *   after:2026-01-01 before:2026-11-01
 * Invalid operators are ignored with an explanatory error; never interpreted as deletion commands.
 */
fun parseSearchQuery(query: String): SearchPlan {
    val (tokens, unclosedQuote) = tokenizeSearch(query)
    val errors = mutableListOf<String>()
    val rules = mutableListOf<SearchRule>()
    if (unclosedQuote) errors.add("Unclosed quotation mark")
    val kinds = mapOf(
        "image" to FileKind.IMAGE, "images" to FileKind.IMAGE, "photo" to FileKind.IMAGE,
        "video" to FileKind.VIDEO, "audio" to FileKind.AUDIO,
        "document" to FileKind.DOCUMENT, "documents" to FileKind.DOCUMENT,
        "archive" to FileKind.ARCHIVE, "installer" to FileKind.INSTALLER,
        "apk" to FileKind.INSTALLER, "other" to FileKind.OTHER
    )

    for (token in tokens.take(30)) {
        val excluded = token.startsWith('-') && token.length > 1
        val body = if (excluded) token.drop(1) else token
        val colon = body.indexOf(':')
        val matcher: SearchMatcher? = when {
            body.startsWith("size>", ignoreCase = true) ->
                parseSize(body.drop(5))?.let { SearchMatcher.Size(it, greater = true) }
            body.startsWith("size<", ignoreCase = true) ->
                parseSize(body.drop(5))?.let { SearchMatcher.Size(it, greater = false) }
            colon > 0 -> {
                val operand = body.substring(colon + 1)
                when (body.take(colon).lowercase(Locale.ROOT)) {
                    "ext" -> operand.split(',').map { it.trim().removePrefix(".").lowercase(Locale.ROOT) }
                        .filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
                        ?.let { SearchMatcher.Extension(it.toSet()) }
                    "type" -> kinds[operand.lowercase(Locale.ROOT)]?.let(SearchMatcher::Kind)
                    "name" -> operand.takeIf { it.isNotEmpty() }?.let(SearchMatcher::Name)
                    "path", "in" -> operand.takeIf { it.isNotEmpty() }?.let(SearchMatcher::Path)
                    "after", "before" -> {
                        val date = try { LocalDate.parse(operand) }
                        catch (_: DateTimeParseException) { null }
                        date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                            ?.let { SearchMatcher.DateRange(it, body.startsWith("after:", true)) }
                    }
                    else -> null
                }
            }
            else -> SearchMatcher.Text(body)
        }
        if (matcher == null) errors.add("Invalid search term: $token")
        else rules.add(SearchRule(excluded, matcher))
    }
    if (tokens.size > 30) errors.add("Only the first 30 terms are used")
    return SearchPlan(rules, errors)
}
