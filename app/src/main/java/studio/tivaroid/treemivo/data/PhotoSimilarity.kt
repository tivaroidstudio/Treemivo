package studio.tivaroid.treemivo.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.CancellationSignal
import android.util.Size
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import kotlin.math.abs

data class SimilarPhotoGroup(val files: List<FileRecord>)

/** Uses the 64-bit difference hash. Near-duplicate suggestions are NOT proof of identity. */
object DifferenceHash {
    fun fromLuminance(pixels: IntArray): Long {
        require(pixels.size == 72) { "Expected 9x8 grayscale pixels" }
        var result = 0L
        for (y in 0 until 8) for (x in 0 until 8) {
            result = (result shl 1) or
                if (pixels[y * 9 + x] > pixels[y * 9 + x + 1]) 1L else 0L
        }
        return result
    }

    fun distance(first: Long, second: Long): Int =
        java.lang.Long.bitCount(first xor second)
}

class PhotoSimilarityAnalyzer(private val resolver: ContentResolver) {
    private data class Candidate(
        val file: FileRecord, val hash: Long, val width: Int, val height: Int
    )

    /** Explicit on-demand Pro analysis; bounded to protect memory, battery and storage access. */
    suspend fun analyze(
        files: List<FileRecord>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<SimilarPhotoGroup> {
        val chosen = files.asSequence()
            .filter { it.kind == FileKind.IMAGE && it.sizeBytes > 10_000L }
            .sortedByDescending { it.modifiedAtMillis }
            .take(400)
            .toList()
        val candidates = ArrayList<Candidate>()
        chosen.forEachIndexed { index, file ->
            currentCoroutineContext().ensureActive()
            val bitmap = try {
                if (Build.VERSION.SDK_INT >= 29) {
                    resolver.loadThumbnail(file.uri, Size(96, 96), null as CancellationSignal?)
                } else {
                    resolver.openInputStream(file.uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null,
                            BitmapFactory.Options().apply { inSampleSize = 8 })
                    }
                }
            } catch (_: SecurityException) { null }
              catch (_: IOException) { null }
              catch (_: IllegalArgumentException) { null }
            if (bitmap != null) {
                try {
                    val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
                    try {
                        val values = IntArray(72)
                        for (y in 0 until 8) for (x in 0 until 9) {
                            val px = scaled.getPixel(x, y)
                            values[y * 9 + x] = (299 * Color.red(px) +
                                587 * Color.green(px) + 114 * Color.blue(px)) / 1000
                        }
                        candidates.add(Candidate(file, DifferenceHash.fromLuminance(values),
                            bitmap.width, bitmap.height))
                    } finally {
                        if (scaled !== bitmap) scaled.recycle()
                    }
                } finally { bitmap.recycle() }
            }
            onProgress(index + 1, chosen.size)
        }
        val parents = IntArray(candidates.size) { it }
        fun root(i: Int): Int {
            var n = i
            while (n != parents[n]) { parents[n] = parents[parents[n]]; n = parents[n] }
            return n
        }
        for (i in candidates.indices) {
            if (i % 16 == 0) currentCoroutineContext().ensureActive()
            val a = candidates[i]
            for (j in i + 1 until candidates.size) {
                val b = candidates[j]
                // Thumbnail aspect ratios are approximate; avoid obvious portrait/landscape mismatch.
                val ratioA = a.width.toDouble() / a.height.coerceAtLeast(1)
                val ratioB = b.width.toDouble() / b.height.coerceAtLeast(1)
                if (abs(ratioA - ratioB) > 0.10) continue
                if (DifferenceHash.distance(a.hash, b.hash) <= 6) {
                    val ra = root(i); val rb = root(j)
                    if (ra != rb) parents[rb] = ra
                }
            }
        }
        return candidates.indices.groupBy { root(it) }.values
            .filter { it.size > 1 }
            .map { indexes ->
                SimilarPhotoGroup(indexes.map { candidates[it].file }
                    .sortedByDescending { it.sizeBytes })
            }
            .sortedByDescending { group -> group.files.sumOf { it.sizeBytes } }
    }
}
