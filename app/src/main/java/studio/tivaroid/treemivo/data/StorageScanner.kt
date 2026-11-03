package studio.tivaroid.treemivo.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.security.MessageDigest

/**
 * ContentProvider-based scanner for a persisted ACTION_OPEN_DOCUMENT_TREE grant.
 *
 * Avoids broad storage permissions. Some providers do not report document size; those
 * documents are displayed as zero bytes, never an invented size.
 */
class StorageScanner(private val resolver: ContentResolver) {
    companion object {
        private const val MAX_ENTRIES = 100_000
        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
    }

    private data class PendingFolder(val documentId: String, val path: String)
    private data class MutableFolder(
        val name: String, val path: String,
        var sizeBytes: Long = 0, var files: Int = 0
    )

    suspend fun scan(treeUri: Uri, onProgress: (Int) -> Unit = {}): StorageReport {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
        val rootName = resolver.query(
            rootUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null
        )?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "Selected folder"

        val queue = ArrayDeque<PendingFolder>()
        queue.add(PendingFolder(rootDocumentId, ""))
        val files = ArrayList<FileRecord>()
        val folders = linkedMapOf<String, MutableFolder>()
        var visited = 0
        var partial = false

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri, current.documentId
            )
            resolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (cursor.moveToNext()) {
                    if (++visited % 256 == 0) {
                        currentCoroutineContext().ensureActive()
                        onProgress(visited)
                    }
                    if (visited > MAX_ENTRIES) {
                        partial = true
                        break
                    }
                    val id = cursor.getString(idColumn) ?: continue
                    val name = cursor.getString(nameColumn) ?: continue
                    val mime = cursor.getString(mimeColumn) ?: "application/octet-stream"
                    val path = if (current.path.isEmpty()) name else "${current.path}/$name"
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        folders.putIfAbsent(path, MutableFolder(name, path))
                        queue.add(PendingFolder(id, path))
                    } else {
                        val size = if (cursor.isNull(sizeColumn)) 0L else cursor.getLong(sizeColumn).coerceAtLeast(0L)
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        files.add(FileRecord(name, path, size, mime, fileUri,
                            if (cursor.isNull(modifiedColumn)) 0L else cursor.getLong(modifiedColumn),
                            FileSource.SAF))
                        // Roll up each file into every ancestor, not just the immediate parent.
                        var parent = current.path
                        while (parent.isNotEmpty()) {
                            val folder = folders.getOrPut(parent) {
                                MutableFolder(parent.substringAfterLast('/'), parent)
                            }
                            folder.sizeBytes += size
                            folder.files++
                            parent = parent.substringBeforeLast('/', "")
                        }
                    }
                }
            }
            if (partial) break
        }

        onProgress(visited)
        return StorageReport(
            rootName = rootName,
            files = files,
            folders = folders.values.map { FolderRecord(it.name, it.path, it.sizeBytes, it.files) }
                .sortedByDescending { it.sizeBytes },
            partial = partial
        )
    }

    /**
     * Exact duplicate check. Equal sizes are only candidates: SHA-256 must also match.
     * This runs only at the user's request because reading large files can take time.
     */
    fun findExactDuplicates(files: List<FileRecord>): List<DuplicateGroup> {
        val candidates = files.filter { it.sizeBytes > 0 }
            .groupBy { it.sizeBytes }
            .values.filter { it.size > 1 }.flatten()
        val hashes = linkedMapOf<Pair<Long, String>, MutableList<FileRecord>>()
        for (file in candidates) {
            val digest = hash(file.uri) ?: continue
            hashes.getOrPut(file.sizeBytes to digest) { mutableListOf() }.add(file)
        }
        return hashes.values.filter { it.size > 1 }
            .map { DuplicateGroup(it.toList()) }
            .sortedByDescending { it.reclaimableBytes }
    }

    private fun hash(uri: Uri): String? {
        return try {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val stream = resolver.openInputStream(uri) ?: return null
        stream.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) messageDigest.update(buffer, 0, n)
            }
        }
        messageDigest.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        null // Unreadable documents cannot be claimed as confirmed duplicates.
        }
    }
}
