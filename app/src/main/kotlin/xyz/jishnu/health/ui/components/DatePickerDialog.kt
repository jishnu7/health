package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.jishnu.health.ui.theme.IntermTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Material3 date-picker wrapped in the app's card-shaped dialog. The picker
 * works in UTC millis internally; we convert to/from [LocalDate] at the
 * edges so the caller never has to think about timezones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val c = IntermTheme.colors
    val initialMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    // Use the M3 DatePicker's own container colour for the card so the
    // calendar grid blends into the card edge instead of sitting on a
    // different shade of surface.
    val pickerContainerColor = DatePickerDefaults.colors().containerColor

    Dialog(
        onDismissRequest = onDismiss,
        // Bypass the default ~280dp dialog cap so the card can stretch to the
        // screen's width margin and the DatePicker inside fills it.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Card padding is applied per child so the M3 DatePicker can sit
        // flush against the card's rounded edge — only the title and the
        // button row get the 20dp horizontal inset.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(pickerContainerColor),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Select date",
                style = IntermTheme.typography.headerTitle,
                color = c.ink,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
            )
            DatePicker(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                title = null,
                headline = null,
                showModeToggle = false,
            )
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IntermButton(onClick = onDismiss, variant = IntermButtonVariant.Ghost, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                IntermButton(
                    onClick = {
                        val millis = state.selectedDateMillis
                        if (millis != null) {
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onConfirm(picked)
                        } else {
                            onDismiss()
                        }
                    },
                    variant = IntermButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                ) { Text("OK") }
            }
        }
    }
}
