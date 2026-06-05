package xyz.jishnu.health.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ShareCapture"

/**
 * Capture handle for a [CaptureBox]. Wraps a Compose [GraphicsLayer] — the
 * official capture path since Compose 1.7. The layer records the composable's
 * draw operations every frame and can be replayed to an [ImageBitmap] off the
 * UI tree, so the share button can pull the latest pixels regardless of when
 * the user taps.
 */
/**
 * Warm light-grey backing for the share image — chosen to give the white card
 * a clear edge so it reads as a discrete artifact instead of blending into a
 * white canvas.
 */
private val ShareBackgroundColor: Int = AndroidColor.rgb(0xEE, 0xEA, 0xE2)

class CardCapture internal constructor(internal val layer: GraphicsLayer) {
    suspend fun captureBitmap(paddingPx: Int, backgroundColor: Int = ShareBackgroundColor): Bitmap? {
        val sourceBitmap = try {
            layer.toImageBitmap().asAndroidBitmap()
        } catch (t: Throwable) {
            Log.e(TAG, "toImageBitmap failed", t)
            return null
        }
        val cardW = sourceBitmap.width
        val cardH = sourceBitmap.height
        if (cardW <= 0 || cardH <= 0) {
            Log.e(TAG, "Captured bitmap has zero size: ${cardW}x${cardH}")
            return null
        }
        // Hardware bitmaps can't be drawn into another canvas — we need a
        // mutable software-backed copy before compositing with padding.
        val softwareSource = if (sourceBitmap.config == Bitmap.Config.HARDWARE) {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            sourceBitmap
        }
        val totalW = cardW + paddingPx * 2
        val totalH = cardH + paddingPx * 2
        val out = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(softwareSource, paddingPx.toFloat(), paddingPx.toFloat(), null)
        return out
    }
}

@Composable
fun rememberCardCapture(): CardCapture {
    val layer = rememberGraphicsLayer()
    return CardCapture(layer)
}

@Composable
fun CaptureBox(
    capture: CardCapture,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layer = capture.layer
    Box(
        modifier = modifier.drawWithContent {
            layer.record {
                this@drawWithContent.drawContent()
            }
            drawLayer(layer)
        },
    ) {
        content()
    }
}

/**
 * Write [bitmap] into the app's shared-cache directory and fire a system share
 * sheet for an `image/png`. Any failure (file write, FileProvider mismatch, no
 * receiver app) is logged + toasted instead of crashing — the share path is a
 * convenience and we'd rather degrade gracefully.
 */
fun shareBitmapAsImage(
    context: Context,
    bitmap: Bitmap,
    chooserTitle: String = "Share fast",
) {
    try {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "fast-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    } catch (t: Throwable) {
        Log.e(TAG, "Share failed", t)
        Toast.makeText(context, "Couldn't share: ${t.message}", Toast.LENGTH_LONG).show()
    }
}
