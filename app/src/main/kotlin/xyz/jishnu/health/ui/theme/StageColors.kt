package xyz.jishnu.health.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import xyz.jishnu.health.data.constants.Stages
import xyz.jishnu.health.data.model.Stage
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Stage colour palette tied to each metabolic stage's `hue` value, mirroring
 * the `--stage-*` OKLCH tokens declared in `docs/project/styles.css`. The OKLCH
 * triple is fixed by theme (light vs dark) and only the hue rotates between
 * stages, so the palette is precomputed once and looked up by stage id.
 */
@Composable
fun stageColors(): Map<String, Color> {
    val dark = IntermTheme.colors.isDark
    return remember(dark) {
        val L = if (dark) 0.75f else 0.56f
        val C = if (dark) 0.11f else 0.105f
        Stages.all.associate { stage -> stage.id to oklchToColor(L, C, stage.hue.toFloat()) }
    }
}

@Composable
fun stageColor(stage: Stage): Color = stageColors()[stage.id] ?: IntermTheme.colors.primary

@Composable
fun stageColorById(id: String): Color = stageColors()[id] ?: IntermTheme.colors.primary

private fun oklchToColor(L: Float, C: Float, hueDeg: Float): Color {
    val rad = Math.toRadians(hueDeg.toDouble())
    val a = C * cos(rad).toFloat()
    val b = C * sin(rad).toFloat()

    val lDash = L + 0.3963377774f * a + 0.2158037573f * b
    val mDash = L - 0.1055613458f * a - 0.0638541728f * b
    val sDash = L - 0.0894841775f * a - 1.2914855480f * b

    val l = lDash * lDash * lDash
    val m = mDash * mDash * mDash
    val s = sDash * sDash * sDash

    val rLinear = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
    val gLinear = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
    val bLinear = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s

    return Color(
        red = encodeSrgb(rLinear).coerceIn(0f, 1f),
        green = encodeSrgb(gLinear).coerceIn(0f, 1f),
        blue = encodeSrgb(bLinear).coerceIn(0f, 1f),
        alpha = 1f,
    )
}

private fun encodeSrgb(linear: Float): Float {
    val x = linear.coerceAtLeast(0f)
    return if (x <= 0.0031308f) 12.92f * x else 1.055f * x.toDouble().pow(1.0 / 2.4).toFloat() - 0.055f
}
