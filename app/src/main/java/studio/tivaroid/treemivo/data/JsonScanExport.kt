package studio.tivaroid.treemivo.data

import android.content.Context
import android.net.Uri
import android.util.JsonWriter
import java.io.IOException
import java.io.OutputStreamWriter

/** A streaming, user-initiated JSON export. Files are never transmitted by the app. */
object JsonScanExport {
    fun write(context: Context, destination: Uri, report: StorageReport) {
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: throw IOException("Cannot open JSON destination")
        output.use { stream ->
            JsonWriter(OutputStreamWriter(stream, Charsets.UTF_8).buffered()).use { json ->
                json.setIndent("  ")
                json.beginObject()
                json.name("application").value("Treemivo")
                json.name("formatVersion").value(1L)
                json.name("generatedAtMillis").value(System.currentTimeMillis())
                json.name("indexedSource").value(report.rootName)
                json.name("partial").value(report.partial)
                json.name("indexedBytes").value(report.totalBytes)
                json.name("indexedFileCount").value(report.files.size.toLong())
                json.name("scopeNotice").value(
                    "Only indexed media or user-authorized folders are included, not the entire device"
                )
                json.name("fileTypes").beginArray()
                for (kind in FileKind.entries) {
                    val data = report.files.filter { it.kind == kind }
                    json.beginObject()
                    json.name("kind").value(kind.name)
                    json.name("count").value(data.size.toLong())
                    json.name("bytes").value(data.sumOf { it.sizeBytes })
                    json.endObject()
                }
                json.endArray()
                json.name("files").beginArray()
                for (file in report.files) {
                    json.beginObject()
                    json.name("name").value(file.name)
                    json.name("relativePath").value(file.relativePath)
                    json.name("sizeBytes").value(file.sizeBytes)
                    json.name("mimeType").value(file.mimeType)
                    json.name("modifiedAtMillis").value(file.modifiedAtMillis)
                    json.name("source").value(file.source.name)
                    json.endObject()
                }
                json.endArray()
                json.endObject()
            }
        }
    }
}
