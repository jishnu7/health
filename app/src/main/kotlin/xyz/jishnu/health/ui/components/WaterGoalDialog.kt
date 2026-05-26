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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.WaterMath
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun WaterGoalDialog(
    currentMl: Int,
    units: Units,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val c = IntermTheme.colors
    val initial = WaterMath.fmtVolume(currentMl, units).value
    var text by remember { mutableStateOf(initial) }
    val unitLabel = if (units == Units.Metric) "ml" else "fl oz"

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(c.card)
                .padding(20.dp),
        ) {
            Text(
                "Daily water goal",
                style = IntermTheme.typography.headerTitle,
                color = c.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Stored as milliliters; this field uses your display unit.",
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
                Text(
                    unitLabel,
                    style = IntermTheme.typography.body.copy(fontSize = 14.sp),
                    color = c.muted,
                )
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
                            onConfirm(WaterMath.typedToMl(n, units))
                        }
                    },
                    variant = IntermButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}
