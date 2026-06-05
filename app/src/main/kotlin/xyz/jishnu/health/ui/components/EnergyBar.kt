package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.ui.theme.IntermTheme
import kotlin.math.max

/**
 * Four-phase energy-utilisation model from docs/project/energy-bar.jsx.
 * Bands are fixed against the 24-hour fast timeline regardless of the user's
 * goal — they represent metabolic state, not goal progress.
 */
data class EnergyPhase(
    val id: String,
    val label: String,
    val startH: Int,
    val endH: Int,
    val message: String,
)

val EnergyPhases: List<EnergyPhase> = listOf(
    EnergyPhase("digest", "Digestion", 0, 4, "Digestion slowing"),
    EnergyPhase("glyco", "Glycogen Burn", 4, 12, "Stored carbs powering your fast"),
    EnergyPhase("trans", "Transition", 12, 16, "Stored energy usage increasing"),
    EnergyPhase("stored", "Stored Energy", 16, 24, "High fat utilization phase"),
)

private data class EnergyPalette(
    val digest: Color,
    val glyco: Color,
    val trans: Color,
    val stored: Color,
) {
    fun colorFor(id: String): Color = when (id) {
        "digest" -> digest
        "glyco" -> glyco
        "trans" -> trans
        else -> stored
    }
}

@Composable
private fun energyPalette(): EnergyPalette {
    val dark = IntermTheme.colors.isDark
    return if (dark) {
        EnergyPalette(
            digest = Color(0xFFE89074),
            glyco = Color(0xFFD9A35A),
            trans = Color(0xFF8BB070),
            stored = Color(0xFF7DD3A8),
        )
    } else {
        EnergyPalette(
            digest = Color(0xFFD97757),
            glyco = Color(0xFFBF8638),
            trans = Color(0xFF5F7D4A),
            stored = Color(0xFF2A4D3E),
        )
    }
}

fun activeEnergyPhase(elapsedHours: Double): EnergyPhase {
    var current = EnergyPhases.first()
    for (p in EnergyPhases) if (elapsedHours >= p.startH) current = p
    return current
}

fun nextEnergyPhase(elapsedHours: Double): EnergyPhase? =
    EnergyPhases.firstOrNull { it.startH > elapsedHours }

private fun fmtHM(h: Double): String {
    val total = max(0.0, h)
    val hh = total.toInt()
    val mm = ((total - hh) * 60).toInt()
    val (h2, m2) = if (mm >= 60) (hh + 1) to (mm - 60) else hh to mm
    return "${h2}h ${m2.toString().padStart(2, '0')}m"
}

/**
 * Focus-card energy bar (Var C in docs/project/energy-bar.jsx). The active
 * phase becomes the hero panel — tinted to the phase colour with the message
 * as the headline — and a compact 4-segment ribbon plus "next phase"
 * countdown sit underneath.
 */
@Composable
fun EnergyBar(
    elapsedHours: Double,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /**
     * When true, hide the tinted hero panel and the "Next phase in …" row,
     * leaving only the 4-segment ribbon with phase names. Used on day detail
     * where the surrounding context already conveys what the current state is.
     */
    compact: Boolean = false,
) {
    val c = IntermTheme.colors
    val palette = energyPalette()
    val active = activeEnergyPhase(elapsedHours)
    val next = nextEnergyPhase(elapsedHours)
    val activeColor = palette.colorFor(active.id)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        if (!compact) {
            // Hero — tinted panel with phase label + big message.
            val heroBg = lerp(c.card, activeColor, 0.12f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(heroBg)
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(activeColor),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        active.label.uppercase(),
                        style = IntermTheme.typography.hEyebrow,
                        color = c.ink2,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    active.message,
                    style = IntermTheme.typography.headerTitle.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.W500,
                        lineHeight = 25.sp,
                    ),
                    color = c.ink,
                )
            }
        }

        // Ribbon — compact 4-segment progress with phase names below.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = if (compact) 16.dp else 14.dp,
                    bottom = if (compact) 16.dp else 16.dp,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EnergyPhases.forEach { p ->
                    val color = palette.colorFor(p.id)
                    val span = (p.endH - p.startH).toDouble()
                    val fill = ((elapsedHours - p.startH) / span)
                        .coerceIn(0.0, 1.0)
                        .toFloat()
                    val isActive = p.id == active.id
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color.copy(alpha = 0.18f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fill)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color),
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            p.label,
                            style = IntermTheme.typography.caption.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isActive) FontWeight.W600 else FontWeight.W500,
                            ),
                            color = if (isActive) c.ink else c.muted,
                        )
                    }
                }
            }

            if (!compact) {
                Spacer(Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.border))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (next != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Next: ",
                                style = IntermTheme.typography.caption,
                                color = c.muted,
                            )
                            Text(
                                next.label,
                                style = IntermTheme.typography.caption.copy(fontWeight = FontWeight.W500),
                                color = c.ink2,
                            )
                        }
                        Text(
                            "in ${fmtHM(next.startH - elapsedHours)}",
                            style = IntermTheme.typography.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.W500),
                            color = activeColor,
                        )
                    } else {
                        Text(
                            "Deepest phase reached",
                            style = IntermTheme.typography.caption.copy(fontWeight = FontWeight.W500),
                            color = c.ink2,
                        )
                    }
                }
            }
        }
    }
}
