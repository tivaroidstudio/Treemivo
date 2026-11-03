package studio.tivaroid.treemivo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import studio.tivaroid.treemivo.R

/** Resize the right-hand inspector on Android desktop/DeX; keyboard buttons are also available. */
@Composable
fun DesktopPaneResize(
    width: Dp,
    minWidth: Dp,
    maxWidth: Dp,
    onWidth: (Dp) -> Unit
) {
    val density = LocalDensity.current
    val liveWidth by rememberUpdatedState(width)
    val liveOnWidth by rememberUpdatedState(onWidth)
    Column(
        Modifier.width(18.dp).fillMaxHeight().background(TreemivoBackground)
            .pointerInput(minWidth, maxWidth, density) {
                detectDragGestures { change, drag ->
                    change.consume()
                    with(density) {
                        liveOnWidth((liveWidth - drag.x.toDp()).coerceIn(minWidth, maxWidth))
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.DragHandle,
            contentDescription = stringResource(R.string.pane_resize),
            tint = TreemivoMuted,
            modifier = Modifier.size(15.dp)
        )
    }
}

/** Accessible alternative to dragging a thin pointer target. */
@Composable
fun DesktopPaneWidthButtons(
    width: Dp, minWidth: Dp, maxWidth: Dp, onWidth: (Dp) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.pane_details), color = TreemivoMuted,
            modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        TextButton(
            onClick = { onWidth((width - 40.dp).coerceAtLeast(minWidth)) },
            enabled = width > minWidth
        ) { Text("−") }
        TextButton(
            onClick = { onWidth((width + 40.dp).coerceAtMost(maxWidth)) },
            enabled = width < maxWidth
        ) { Text("+") }
    }
}
