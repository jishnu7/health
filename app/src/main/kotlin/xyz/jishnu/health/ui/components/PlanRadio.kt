package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.data.model.Plan
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun PlanRadio(plan: Plan, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val c = IntermTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.primarySoft else c.card)
            .border(1.5.dp, if (selected) c.primary else c.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.bg)
                .border(1.dp, c.border, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                plan.label,
                style = IntermTheme.typography.mono.copy(fontSize = 16.sp, fontWeight = FontWeight.W600),
                color = c.ink,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${plan.fastHours}h fasting",
                style = IntermTheme.typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.W500),
                color = c.ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${plan.subtitle} · ${plan.eatHours}h eating window",
                style = IntermTheme.typography.caption,
                color = c.muted,
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) c.primary else Color.Transparent)
                .border(1.5.dp, if (selected) c.primary else c.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(IntermIcons.Check, contentDescription = null, tint = c.surface, modifier = Modifier.size(14.dp))
        }
    }
}
