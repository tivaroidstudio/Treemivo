package studio.tivaroid.treemivo.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Reads Android's indexed image/video/audio collections without broad file access. */
class MediaScanner(private val context: Context) {
    private fun permitted(name: String) =
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED

    fun hasAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= 33)
            permitted(Manifest.permission.READ_MEDIA_IMAGES) ||
                permitted(Manifest.permission.READ_MEDIA_VIDEO) ||
                permitted(Manifest.permission.READ_MEDIA_AUDIO) ||
                (Build.VERSION.SDK_INT >= 34 &&
                    permitted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED"))
        else permitted(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun partialVisualAccess(): Boolean =
        Build.VERSION.SDK_INT >= 34 &&
            permitted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED") &&
            !permitted(Manifest.permission.READ_MEDIA_IMAGES) &&
            !permitted(Manifest.permission.READ_MEDIA_VIDEO)

    suspend fun scan(onProgress: (Int) -> Unit = {}): StorageReport {
        val legacy = Build.VERSION.SDK_INT < 33 &&
            permitted(Manifest.permission.READ_EXTERNAL_STORAGE)
        val partialVisual = partialVisualAccess()
        val sources = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to (
                legacy || (Build.VERSION.SDK_INT >= 33 &&
                    permitted(Manifest.permission.READ_MEDIA_IMAGES)) || partialVisual),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to (
                legacy || (Build.VERSION.SDK_INT >= 33 &&
                    permitted(Manifest.permission.READ_MEDIA_VIDEO)) || partialVisual),
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to (
                legacy || (Build.VERSION.SDK_INT >= 33 &&
                    permitted(Manifest.permission.READ_MEDIA_AUDIO)))
        )
        val result = ArrayList<FileRecord>()
        var incomplete = partialVisual
        var visited = 0
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        if (Build.VERSION.SDK_INT >= 29) projection.add(MediaStore.MediaColumns.RELATIVE_PATH)
        for ((collection, granted) in sources) {
            if (!granted) continue
            currentCoroutineContext().ensureActive()
            try {
                context.contentResolver.query(
                    collection, projection.toTypedArray(), null, null, null
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    val pathIndex = if (Build.VERSION.SDK_INT >= 29)
                        cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
                    while (cursor.moveToNext()) {
                        if (++visited % 256 == 0) {
                            currentCoroutineContext().ensureActive()
                            onProgress(visited)
                        }
                        if (result.size >= 100_000) {
                            incomplete = true
                            break
                        }
                        val name = cursor.getString(nameIndex) ?: continue
                        val path = if (pathIndex >= 0 && !cursor.isNull(pathIndex))
                            cursor.getString(pathIndex).orEmpty().trim('/') else ""
                        val size = if (cursor.isNull(sizeIndex)) 0L else cursor.getLong(sizeIndex)
                        val modified = if (cursor.isNull(dateIndex)) 0L else cursor.getLong(dateIndex)
                        result.add(
                            FileRecord(
                                name = name,
                                relativePath = if (path.isEmpty()) name else "$path/$name",
                                sizeBytes = size.coerceAtLeast(0L),
                                mimeType = cursor.getString(mimeIndex) ?: "application/octet-stream",
                                uri = ContentUris.withAppendedId(collection, cursor.getLong(idIndex)),
                                modifiedAtMillis = modified * 1000L,
                                source = FileSource.MEDIA
                            )
                        )
                    }
                }
            } catch (_: SecurityException) {
                incomplete = true
            } catch (_: IllegalArgumentException) {
                incomplete = true
            }
            if (result.size >= 100_000) break
        }
        onProgress(visited)
        return StorageReport("MediaStore", result, folderBreakdown(result), incomplete)
    }
}
