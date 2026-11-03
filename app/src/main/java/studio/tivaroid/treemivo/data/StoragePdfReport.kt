package studio.tivaroid.treemivo.data

import android.content.Context
import android.net.Uri
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.IOException
import java.text.DateFormat
import java.util.Date

/** Offline summary report, explicitly exported to a user-chosen document URI. */
object StoragePdfReport {
    fun write(context: Context, destination: Uri, report: StorageReport) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        fun line(value: String, y: Float, title: Boolean = false) {
            paint.textSize = if (title) 20f else 12f
            paint.isFakeBoldText = title
            canvas.drawText(value.take(90), 36f, y, paint)
        }
        try {
            line("Treemivo - Storage Report", 54f, true)
            line(DateFormat.getDateTimeInstance().format(Date()), 81f)
            line("Source: ${report.rootName}", 107f)
            line("Indexed files: ${report.files.size}", 129f)
            line("Indexed bytes: ${report.totalBytes}", 151f)
            line(if (report.partial) "PARTIAL SCAN - limited permission or scan cap" else
                "Source completed; scope may not cover entire device", 173f)
            line("File types", 210f, true)
            val stats = report.files.groupBy { it.kind }
                .mapValues { (_, files) -> files.sumOf { it.sizeBytes } }
            var y = 235f
            FileKind.entries.forEach { kind ->
                line("${kind.name}: ${stats[kind] ?: 0L} bytes", y); y += 20f
            }
            y += 22f
            line("Largest indexed files", y, true); y += 25f
            report.files.sortedByDescending { it.sizeBytes }.take(16).forEach {
                line("${it.name.take(60)} - ${it.sizeBytes} B", y); y += 23f
            }
            line("Report includes only files accessible to Treemivo.", 815f)
            document.finishPage(page)
            context.contentResolver.openOutputStream(destination, "wt")?.use {
                document.writeTo(it)
            } ?: throw IOException("Cannot open PDF destination")
        } finally { document.close() }
    }
}
