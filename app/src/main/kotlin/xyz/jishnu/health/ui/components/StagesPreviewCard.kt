package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.data.constants.Stages
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.ui.theme.stageColors

/**
 * Not-fasting teaser for the home screen: a tinted-primary header, the 24h
 * stage ribbon shown flat (no progress fill), and a caption nudging into the
 * stages screen. Mirrors `StagesPreviewCard` in `docs/project/shared.jsx`.
 */
@Composable
fun StagesPreviewCard(
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
) {
    val c = IntermTheme.colors
    val palette = stageColors()
    val segs = Stages.all.filter { it.startHour < 24 }
    val ends = segs.mapIndexed { idx, s ->
        val end = segs.getOrNull(idx + 1)?.startHour?.toFloat() ?: 24f
        Triple(s.id, s.startHour.toFloat(), end)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .let { if (onOpen != null) it.clickable(onClick = onOpen) else it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(lerp(c.card, c.primary, 0.10f))
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("METABOLIC STAGES", style = IntermTheme.typography.hEyebrow, color = c.ink2)
                if (onOpen != null) {
                    Icon(IntermIcons.Chevron, contentDescription = null, tint = c.muted)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Preview the journey ahead",
                style = IntermTheme.typography.headerTitle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                ),
                color = c.ink,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                ends.forEach { (id, start, end) ->
                    val span = (end - start).coerceAtLeast(0.001f)
                    val segColor = palette[id] ?: c.primary
                    Box(
                        modifier = Modifier
                            .weight(span)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(segColor.copy(alpha = 0.9f)),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Eight stages, from fed to deep ketosis & autophagy.",
                style = IntermTheme.typography.caption,
                color = c.muted,
            )
        }
    }
}
