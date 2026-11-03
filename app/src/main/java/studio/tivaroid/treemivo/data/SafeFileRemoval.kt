package studio.tivaroid.treemivo.data

import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi

/** No automatic deletion. The caller must display a second confirmation for SAF files. */
object SafeFileRemoval {
    fun supportsTrash(file: FileRecord): Boolean =
        Build.VERSION.SDK_INT >= 30 && file.source == FileSource.MEDIA &&
            file.uri.authority == MediaStore.AUTHORITY

    @RequiresApi(30)
    fun trashRequest(context: Context, file: FileRecord): IntentSender {
        require(supportsTrash(file))
        return MediaStore.createTrashRequest(
            context.contentResolver, listOf(file.uri), true
        ).intentSender
    }

    /** System prompt handles these items as one user-approved transaction (API 30+). */
    @RequiresApi(30)
    fun trashBatchRequest(context: Context, files: List<FileRecord>): IntentSender {
        require(files.isNotEmpty() && files.size <= 500)
        require(files.all(::supportsTrash))
        require(files.map { it.uri.toString() }.toSet().size == files.size)
        return MediaStore.createTrashRequest(
            context.contentResolver, files.map { it.uri }, true
        ).intentSender
    }

    fun supportsSafDelete(resolver: ContentResolver, file: FileRecord): Boolean {
        if (file.source != FileSource.SAF) return false
        return try {
            resolver.query(file.uri,
                arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null
            )?.use {
                it.moveToFirst() && !it.isNull(0) &&
                    (it.getInt(0) and DocumentsContract.Document.FLAG_SUPPORTS_DELETE) != 0
            } ?: false
        } catch (_: Exception) { false }
    }

    /** Irreversible and invoked only after explicit destructive confirmation. */
    fun deleteSaf(resolver: ContentResolver, file: FileRecord): Boolean {
        require(file.source == FileSource.SAF && supportsSafDelete(resolver, file))
        return DocumentsContract.deleteDocument(resolver, file.uri)
    }
}
