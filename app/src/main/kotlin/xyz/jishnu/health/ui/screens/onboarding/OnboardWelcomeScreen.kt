package xyz.jishnu.health.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.ProgressRing
import xyz.jishnu.health.ui.components.StepDots3
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun OnboardWelcomeScreen(
    onGetStarted: () -> Unit,
) {
    val c = IntermTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, top = 60.dp, bottom = 28.dp),
        ) {
            Text("WELCOME TO INTERM", style = IntermTheme.typography.hEyebrow, color = c.muted)
            Spacer(Modifier.height(8.dp))
            Text(
                "Track your fasting and your weight.",
                style = IntermTheme.typography.hTitle.copy(fontSize = 36.sp),
                color = c.ink,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "A quiet companion for intermittent fasting. We focus on the duration of your fast and the metabolic phases you move through.",
                style = IntermTheme.typography.body.copy(fontSize = 15.sp),
                color = c.ink2,
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProgressRing(progress = 0.66f, size = 200.dp, stroke = 10.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("STAGE 5", style = IntermTheme.typography.hEyebrow, color = c.muted)
                        Spacer(Modifier.height(4.dp))
                        Text("14:23", style = IntermTheme.typography.hDisplay.copy(fontSize = 32.sp), color = c.ink)
                        Spacer(Modifier.height(2.dp))
                        Text("Fat burn", style = IntermTheme.typography.caption, color = c.muted)
                    }
                }
            }

            StepDots3(total = 5, currentStep = 1)
            Spacer(Modifier.height(18.dp))
            IntermButton(onClick = onGetStarted, variant = IntermButtonVariant.Primary, fillWidth = true) {
                Text("Get Started")
                Icon(IntermIcons.Chevron, contentDescription = null)
            }
        }
    }
}
