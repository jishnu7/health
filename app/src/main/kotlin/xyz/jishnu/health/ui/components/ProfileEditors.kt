package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import xyz.jishnu.health.data.model.Sex
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.ui.theme.IntermTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SexSegmented(
    sex: Sex?,
    onSelect: (Sex) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.border2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SexButton(label = "Male", active = sex == Sex.Male, onClick = { onSelect(Sex.Male) }, modifier = Modifier.weight(1f))
        SexButton(label = "Female", active = sex == Sex.Female, onClick = { onSelect(Sex.Female) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SexButton(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = IntermTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) c.primarySoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = IntermTheme.typography.body.copy(
                fontSize = 14.sp,
                fontWeight = if (active) FontWeight.W600 else FontWeight.W500,
            ),
            color = if (active) c.primary else c.ink2,
        )
    }
}

/**
 * Modal that lets the user enter their height in the active display unit (cm
 * for metric, total inches for imperial — converted back to cm on save).
 */
@Composable
fun HeightDialog(
    currentCm: Double?,
    units: Units,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val c = IntermTheme.colors
    val initialText = remember(currentCm, units) {
        if (currentCm == null) "" else when (units) {
            Units.Metric -> currentCm.toInt().toString()
            Units.Imperial -> kotlin.math.round(currentCm / 2.54).toInt().toString()
        }
    }
    var text by remember { mutableStateOf(initialText) }
    val unitLabel = if (units == Units.Metric) "cm" else "in"

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(c.card)
                .padding(20.dp),
        ) {
            Text("Height", style = IntermTheme.typography.headerTitle, color = c.ink)
            Spacer(Modifier.height(6.dp))
            Text(
                if (units == Units.Metric) "Enter in centimetres."
                else "Enter total inches (e.g., 5'10\" = 70).",
                style = IntermTheme.typography.caption,
                color = c.muted,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.border2)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = LocalTextStyle.current.copy(
                            color = c.ink,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.W500,
                        ),
                        cursorBrush = SolidColor(c.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                Text(unitLabel, style = IntermTheme.typography.body.copy(fontSize = 14.sp), color = c.muted)
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntermButton(onClick = onDismiss, variant = IntermButtonVariant.Ghost, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                IntermButton(
                    onClick = {
                        val n = text.toDoubleOrNull()
                        if (n != null && n > 0.0) {
                            val cm = when (units) {
                                Units.Metric -> n
                                Units.Imperial -> n * 2.54
                            }
                            onConfirm(cm)
                        }
                    },
                    variant = IntermButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateOfBirthDialog(
    currentIso: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initialMillis = currentIso?.let { runCatching { LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli() }.getOrNull() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms ->
                    val date = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                    onConfirm(date.toString())
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

fun formatHeightForDisplay(cm: Double, units: Units): String = when (units) {
    Units.Metric -> "${cm.toInt()} cm"
    Units.Imperial -> {
        val totalIn = cm / 2.54
        val feet = (totalIn / 12).toInt()
        val inches = kotlin.math.round(totalIn - feet * 12).toInt()
        "$feet'${inches}\""
    }
}

fun formatDateOfBirth(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
}.getOrDefault(iso)
