package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
    showDivider: Boolean = true,
) {
    SettingsRowFrame(
        onClick = { onCheckedChange(!checked) },
        showDivider = showDivider,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500), color = IntermTheme.colors.ink)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, style = IntermTheme.typography.caption, color = IntermTheme.colors.muted)
            }
        }
        IntermToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun NavRow(
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    trailing: String? = null,
    showChevron: Boolean = true,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    SettingsRowFrame(onClick = onClick, showDivider = showDivider, modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500), color = IntermTheme.colors.ink)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, style = IntermTheme.typography.caption, color = IntermTheme.colors.muted)
            }
        }
        if (trailing != null) {
            Text(
                trailing,
                style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500),
                color = IntermTheme.colors.muted,
            )
        }
        if (showChevron) {
            Spacer(Modifier.width(8.dp))
            Icon(IntermIcons.Chevron, contentDescription = null, tint = IntermTheme.colors.subtle)
        }
    }
}

@Composable
fun TimeRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsRowFrame(
        onClick = if (enabled) { { showDialog = true } } else null,
        showDivider = showDivider,
        modifier = modifier.alpha(if (enabled) 1f else 0.4f),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500), color = IntermTheme.colors.ink)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, style = IntermTheme.typography.caption, color = IntermTheme.colors.muted)
            }
        }
        Text(
            text = value,
            style = IntermTheme.typography.mono.copy(fontSize = 14.sp, fontWeight = FontWeight.W500),
            color = IntermTheme.colors.muted,
        )
        Spacer(Modifier.width(4.dp))
        Icon(IntermIcons.Chevron, contentDescription = null, tint = IntermTheme.colors.subtle)
    }
    if (showDialog) {
        TimePickerDialog(
            initial = value,
            onDismiss = { showDialog = false },
            onConfirm = { showDialog = false; onValueChange(it) },
        )
    }
}

@Composable
private fun SettingsRowFrame(
    onClick: (() -> Unit)?,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val c = IntermTheme.colors
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .defaultMinSize(minHeight = 60.dp)
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.border))
        }
    }
}
