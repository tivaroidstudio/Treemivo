package studio.tivaroid.treemivo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.tivaroid.treemivo.R
import studio.tivaroid.treemivo.data.StorageGrowth
import studio.tivaroid.treemivo.data.FileKind
import java.util.Locale

/**
 * Shows deltas for equivalent completed indexed scopes only.
 * Not a device storage forecast and not proof of which files were added.
 */
@Composable
fun StorageGrowthPanel(growth: StorageGrowth?, isPro: Boolean) {
    Surface(
        color = TreemivoPanel,
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.growth_title),
                fontWeight = FontWeight.SemiBold)
            if (growth == null) {
                Text(stringResource(R.string.growth_not_comparable),
                    color = TreemivoMuted, fontSize = 12.sp)
            } else {
                val prefix = if (growth.deltaBytes > 0) "+" else ""
                val suffix = if (growth.deltaFiles > 0) "+" else ""
                Text(prefix + prettySigned(growth.deltaBytes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TreemivoAccent,
                    fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.growth_file_difference) + ": " +
                        suffix + growth.deltaFiles +
                        " · " + String.format(Locale.getDefault(),
                            "%.1f", growth.elapsedDays) + " " +
                        stringResource(R.string.growth_days),
                    color = TreemivoMuted, fontSize = 12.sp
                )
                if (isPro) {
                    HorizontalDivider()
                    Text(stringResource(R.string.growth_by_type),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    growth.kindChanges.take(7).forEach { item ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(kindName(item.kind), Modifier.weight(1f),
                                fontSize = 12.sp)
                            val sign = if (item.deltaBytes > 0) "+" else ""
                            Text(sign + prettySigned(item.deltaBytes),
                                color = TreemivoMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(stringResource(R.string.growth_pro_note),
                        color = TreemivoMuted, fontSize = 12.sp)
                }
                Text(stringResource(R.string.growth_scope_note),
                    color = TreemivoMuted, fontSize = 11.sp)
            }
        }
    }
}

private fun kindName(kind: FileKind): String = when (kind) {
    FileKind.IMAGE -> "Images"
    FileKind.VIDEO -> "Video"
    FileKind.AUDIO -> "Audio"
    FileKind.DOCUMENT -> "Documents"
    FileKind.ARCHIVE -> "Archives"
    FileKind.INSTALLER -> "Installers"
    FileKind.OTHER -> "Other"
}

private fun prettySigned(bytes: Long): String {
    val positive = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(bytes)
    var value = positive.toDouble()
    var unit = 0
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    val number = String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
    return if (bytes < 0) "-$number" else number
}
