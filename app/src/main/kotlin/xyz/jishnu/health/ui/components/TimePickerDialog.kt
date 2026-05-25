package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import xyz.jishnu.health.ui.theme.IntermTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val (initHour, initMinute) = initial.split(":").let { it[0].toInt() to it[1].toInt() }
    val state = rememberTimePickerState(initialHour = initHour, initialMinute = initMinute, is24Hour = true)
    val c = IntermTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(c.card)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Select time", style = IntermTheme.typography.headerTitle, color = c.ink)
            TimePicker(state = state)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntermButton(onClick = onDismiss, variant = IntermButtonVariant.Ghost, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                IntermButton(
                    onClick = {
                        val hh = state.hour.toString().padStart(2, '0')
                        val mm = state.minute.toString().padStart(2, '0')
                        onConfirm("$hh:$mm")
                    },
                    variant = IntermButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                ) { Text("OK") }
            }
        }
    }
}
