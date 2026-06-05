package xyz.jishnu.health.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

/**
 * Wraps [LastFastCard] with the offscreen capture pipeline + system share
 * intent in one composable. Internally renders the visible card and a
 * size-matched hidden sibling at alpha 0 that feeds a [GraphicsLayer]
 * continuously, so tapping Share never needs to recompose the visible card
 * (no flash, no frame waits).
 *
 * Used wherever there's already a visible card on screen — Home's returning
 * state and DayDetail's completed-fast block. For contexts where the card
 * isn't shown but we still need a share image (e.g., Home's active state
 * sharing the in-progress fast), pair [rememberCardCapture] with
 * [HiddenLastFastCaptureCard] + [rememberFastShareTrigger].
 */
@Composable
fun ShareableLastFastCard(
    summary: LastFastSummary,
    modifier: Modifier = Modifier,
    edit: LastFastEdit? = null,
    chooserTitle: String = "Share fast",
) {
    val capture = rememberCardCapture()
    val share = rememberFastShareTrigger(capture, chooserTitle)
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(0f),
        ) {
            CaptureBox(capture = capture, modifier = Modifier.fillMaxWidth()) {
                LastFastCard(
                    summary = summary,
                    modifier = Modifier.fillMaxWidth(),
                    forCapture = true,
                )
            }
        }
        LastFastCard(
            summary = summary,
            modifier = Modifier.fillMaxWidth(),
            edit = edit,
            onShare = share,
        )
    }
}

/**
 * Renders a [LastFastCard] (in capture mode) into the [capture] layer without
 * occupying any visible layout space. Used by contexts that don't show a
 * card on screen — e.g., Home's active state — so the share trigger has a
 * pre-recorded layer ready when the user taps Share.
 *
 * The Layout block measures the card at the parent's available width and
 * reports back a 0×0 size to its parent. `clipToBounds` keeps the rendered
 * pixels from leaking through; the `GraphicsLayer` records the unclipped
 * content at full size in parallel.
 *
 * MUST be hosted inside a parent whose maxWidth is bounded — typically a
 * padded Column on a phone-width screen.
 */
@Composable
fun HiddenLastFastCaptureCard(
    summary: LastFastSummary,
    capture: CardCapture,
) {
    Layout(
        modifier = Modifier.clipToBounds(),
        content = {
            CaptureBox(capture = capture, modifier = Modifier.fillMaxWidth()) {
                LastFastCard(
                    summary = summary,
                    modifier = Modifier.fillMaxWidth(),
                    forCapture = true,
                )
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val measureConstraints = Constraints(
            minWidth = width,
            maxWidth = width,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )
        val placeable = measurables.first().measure(measureConstraints)
        layout(0, 0) {
            placeable.place(0, 0)
        }
    }
}

/**
 * Returns a callback that captures the current [capture] layer to a bitmap
 * and fires the Android share sheet for an image/png. The coroutine is
 * scoped to the calling composition; the callback identity is stable across
 * recompositions for the same (capture, chooserTitle) pair.
 */
@Composable
fun rememberFastShareTrigger(
    capture: CardCapture,
    chooserTitle: String = "Share fast",
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(capture, chooserTitle) {
        {
            scope.launch {
                val bitmap = capture.captureBitmap(paddingPx = 48) ?: return@launch
                shareBitmapAsImage(context, bitmap, chooserTitle)
            }
        }
    }
}
