package xyz.jishnu.health.ui.screens.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import xyz.jishnu.health.ui.components.BottomNav
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermSegmented
import xyz.jishnu.health.ui.components.IntermStageChip
import xyz.jishnu.health.ui.components.IntermToggle
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.components.ProgressRing
import xyz.jishnu.health.ui.components.SegmentedOption
import xyz.jishnu.health.ui.components.StageDots
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun ComponentGalleryScreen(modifier: Modifier = Modifier) {
    val c = IntermTheme.colors
    var toggleA by remember { mutableStateOf(true) }
    var toggleB by remember { mutableStateOf(false) }
    var seg by remember { mutableStateOf("16:8") }
    var seg2 by remember { mutableStateOf("lb") }
    var activeTab by remember { mutableStateOf(NavTab.Today) }

    val statusBarPad = WindowInsets.statusBars.asPaddingValues()

    Box(modifier = modifier.background(c.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = statusBarPad.calculateTopPadding(),
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                IntermTopBar(
                    title = "Components",
                    leading = { Icon(IntermIcons.Back, contentDescription = "Back", tint = c.ink2) },
                    trailing = { Icon(IntermIcons.Settings, contentDescription = "Settings", tint = c.ink2) },
                )
            }
            item { Section("Typography") { TypographySection() } }
            item {
                Section("Buttons") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        IntermButton(onClick = {}, variant = IntermButtonVariant.Primary, fillWidth = true) {
                            Text("Start Fasting")
                        }
                        IntermButton(onClick = {}, variant = IntermButtonVariant.Ghost, fillWidth = true) {
                            Text("I Already Have an Account")
                        }
                        IntermButton(onClick = {}, variant = IntermButtonVariant.Soft, fillWidth = true) {
                            Icon(IntermIcons.Check, contentDescription = null)
                            Text("Goal Met")
                        }
                        IntermButton(onClick = {}, variant = IntermButtonVariant.Danger, fillWidth = true) {
                            Icon(IntermIcons.Stop, contentDescription = null)
                            Text("End Fast")
                        }
                    }
                }
            }
            item {
                Section("Card") {
                    IntermCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Fat burn", style = IntermTheme.typography.hTitle, color = c.ink)
                        Text(
                            "You are running primarily on fat. Stored triglycerides break down into fatty acids for fuel.",
                            style = IntermTheme.typography.body,
                            color = c.ink2,
                        )
                    }
                }
            }
            item {
                Section("Toggle") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IntermToggle(checked = toggleA, onCheckedChange = { toggleA = it })
                            Text("Fasting reminders", style = IntermTheme.typography.body, color = c.ink)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IntermToggle(checked = toggleB, onCheckedChange = { toggleB = it })
                            Text("Sticky notification", style = IntermTheme.typography.body, color = c.ink)
                        }
                    }
                }
            }
            item {
                Section("Segmented") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        IntermSegmented(
                            options = listOf(
                                SegmentedOption("14:10", "14:10"),
                                SegmentedOption("16:8", "16:8"),
                                SegmentedOption("18:6", "18:6"),
                                SegmentedOption("20:4", "20:4"),
                            ),
                            selected = seg,
                            onSelect = { seg = it },
                        )
                        IntermSegmented(
                            options = listOf(
                                SegmentedOption("lb", "lb"),
                                SegmentedOption("kg", "kg"),
                            ),
                            selected = seg2,
                            onSelect = { seg2 = it },
                        )
                    }
                }
            }
            item {
                Section("Stage chip") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IntermStageChip(label = "Fat burn · 14–16 h")
                        IntermStageChip(label = "Tap to view", onClick = {})
                    }
                }
            }
            item {
                Section("Progress ring") {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        ProgressRing(progress = 0.25f, size = 96.dp, stroke = 8.dp) {
                            Text("25%", style = IntermTheme.typography.button)
                        }
                        ProgressRing(progress = 0.66f, size = 96.dp, stroke = 8.dp) {
                            Text("66%", style = IntermTheme.typography.button)
                        }
                        ProgressRing(progress = 1f, size = 96.dp, stroke = 8.dp, dashed = true) {
                            Text("100%", style = IntermTheme.typography.button)
                        }
                    }
                }
            }
            item {
                Section("Stage dots") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StageDots(count = 8, currentIdx = 0)
                        StageDots(count = 8, currentIdx = 4)
                        StageDots(count = 8, currentIdx = 7)
                    }
                }
            }
            item {
                Section("Bottom nav") {
                    BottomNav(active = activeTab, onChange = { activeTab = it })
                }
            }
            item {
                Section("Icons") {
                    val icons = listOf(
                        "Home" to IntermIcons.Home,
                        "Scale" to IntermIcons.Scale,
                        "Chart" to IntermIcons.Chart,
                        "History" to IntermIcons.History,
                        "Settings" to IntermIcons.Settings,
                        "Plus" to IntermIcons.Plus,
                        "Minus" to IntermIcons.Minus,
                        "Bell" to IntermIcons.Bell,
                        "Back" to IntermIcons.Back,
                        "Check" to IntermIcons.Check,
                        "Chevron" to IntermIcons.Chevron,
                        "Flame" to IntermIcons.Flame,
                        "Drop" to IntermIcons.Drop,
                        "Food" to IntermIcons.Food,
                        "Stop" to IntermIcons.Stop,
                        "Play" to IntermIcons.Play,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        icons.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { (name, vec) -> IconTile(name = name, icon = vec) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val c = IntermTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = title.uppercase(), style = IntermTheme.typography.hEyebrow, color = c.muted)
        content()
    }
}

@Composable
private fun TypographySection() {
    val c = IntermTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("16:23", style = IntermTheme.typography.hDisplay, color = c.ink)
        Text("Welcome to Interm", style = IntermTheme.typography.hTitle, color = c.ink)
        Text("CURRENT STAGE", style = IntermTheme.typography.hEyebrow, color = c.muted)
        Text("Header title", style = IntermTheme.typography.headerTitle, color = c.ink)
        Text(
            "Your body has shifted into fat burning. Insulin is low and lipolysis is active.",
            style = IntermTheme.typography.body,
            color = c.ink2,
        )
        Text("8h fasted today", style = IntermTheme.typography.caption, color = c.muted)
    }
}

@Composable
private fun IconTile(name: String, icon: ImageVector) {
    val c = IntermTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(c.card, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = c.ink)
        }
        Text(name, style = IntermTheme.typography.caption, color = c.muted)
    }
}
