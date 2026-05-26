package xyz.jishnu.health.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.WaterMath
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.StepDots3
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.screens.water.WaterGlass
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.SettingsViewModel
import xyz.jishnu.health.vm.WeightViewModel
import kotlin.math.roundToInt

private const val ML_PER_KG = 35

@Composable
fun OnboardWaterScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    settingsVm: SettingsViewModel = hiltViewModel(),
    weightVm: WeightViewModel = hiltViewModel(),
) {
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val weightState by weightVm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors
    val units = settings.units

    val latestWeightKg = weightState.previous?.weightKg ?: weightState.draftKg ?: 70.0
    val rawMl = latestWeightKg * ML_PER_KG
    val suggestedMl = remember(rawMl, units) {
        when (units) {
            Units.Metric -> ((rawMl / 100.0).roundToInt() * 100).coerceAtLeast(100)
            Units.Imperial -> {
                val roundedOz = (WaterMath.mlToOz(rawMl.roundToInt()) / 5.0).roundToInt() * 5
                WaterMath.ozToMl(roundedOz.toDouble()).coerceAtLeast(1)
            }
        }
    }

    // The initial input text is derived from the suggested ml in the user's display unit.
    // We track an explicit "user touched the field" flag so the suggestion re-applies if the
    // user comes back after changing units/weight without typing anything.
    var touched by remember { mutableStateOf(false) }
    val suggestedTyped = remember(suggestedMl, units) { WaterMath.fmtVolume(suggestedMl, units).value }
    var text by remember { mutableStateOf(suggestedTyped) }
    LaunchedEffect(suggestedTyped) { if (!touched) text = suggestedTyped }

    val unitLabel = if (units == Units.Metric) "ml" else "fl oz"
    val parsedMl = text.toDoubleOrNull()?.let { WaterMath.typedToMl(it, units) } ?: suggestedMl
    val previewProgress = (parsedMl.toFloat() / suggestedMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    val previewVolume = WaterMath.fmtVolume(parsedMl, units)
    val suggestedVolume = WaterMath.fmtVolume(suggestedMl, units)

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            IntermTopBar(
                title = "",
                leading = {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { Icon(IntermIcons.Back, contentDescription = "Back", tint = c.ink2) }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(top = 8.dp, bottom = 28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column {
                        Text("STEP 4 OF 5", style = IntermTheme.typography.hEyebrow, color = c.muted)
                        Spacer(Modifier.height(8.dp))
                        Text("Set a daily water goal.", style = IntermTheme.typography.hTitle, color = c.ink)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Suggested ${suggestedVolume.value} ${suggestedVolume.unit} based on ${ML_PER_KG} ml per kg of body weight. Tweak if you'd like.",
                            style = IntermTheme.typography.body,
                            color = c.ink2,
                        )
                    }

                    IntermCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            WaterGlass(progress = previewProgress, size = 96.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DAILY GOAL", style = IntermTheme.typography.hEyebrow, color = c.muted)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        previewVolume.value,
                                        style = IntermTheme.typography.hDisplay.copy(fontSize = 36.sp),
                                        color = c.ink,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        previewVolume.unit,
                                        style = IntermTheme.typography.body.copy(fontSize = 14.sp),
                                        color = c.muted,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.border2)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            BasicTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    touched = true
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    color = c.ink,
                                    fontSize = 20.sp,
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
                            modifier = Modifier.width(48.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(c.border2)
                                .clickable {
                                    text = suggestedTyped
                                    touched = false
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(IntermIcons.Check, contentDescription = "Reset", tint = c.ink2)
                        }
                    }
                }

                StepDots3(total = 5, currentStep = 4)
                Spacer(Modifier.height(18.dp))
                IntermButton(
                    onClick = {
                        settingsVm.setWaterGoalMl(parsedMl.coerceAtLeast(1))
                        onContinue()
                    },
                    variant = IntermButtonVariant.Primary,
                    fillWidth = true,
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
