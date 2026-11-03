package studio.tivaroid.treemivo.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.tivaroid.treemivo.R
import studio.tivaroid.treemivo.data.FolderBookmark
import studio.tivaroid.treemivo.data.directoryAncestors

@Composable
fun FolderQuickLinks(
    current: String,
    bookmarks: List<FolderBookmark>,
    pinned: Boolean,
    onNavigate: (String) -> Unit,
    onToggleBookmark: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically) {
            directoryAncestors(current).forEachIndexed { index, path ->
                if (index > 0) Icon(
                    Icons.Outlined.ChevronRight, null, modifier = Modifier.size(14.dp),
                    tint = TreemivoMuted
                )
                TextButton(onClick = { onNavigate(path) }) {
                    Text(
                        if (path.isEmpty()) stringResource(R.string.folder_root)
                        else path.substringAfterLast('/'),
                        fontSize = 12.sp,
                        color = if (path == current) MaterialTheme.colorScheme.primary
                            else TreemivoMuted
                    )
                }
            }
            if (current.isNotEmpty()) {
                IconButton(onClick = { onToggleBookmark(current) }) {
                    Icon(
                        if (pinned) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(
                            if (pinned) R.string.folder_unpin else R.string.folder_pin
                        ), tint = TreemivoAccent
                    )
                }
            }
        }
        if (bookmarks.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.folder_bookmarks), fontSize = 12.sp,
                    color = TreemivoMuted)
                bookmarks.forEach { item ->
                    AssistChip(
                        onClick = { onNavigate(item.path) },
                        label = { Text(item.title, maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Bookmark, null,
                                modifier = Modifier.size(15.dp))
                        }
                    )
                }
            }
        }
    }
}
