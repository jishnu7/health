package xyz.jishnu.health.ui.screens.stages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.model.Stage
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.FastingViewModel

@Composable
fun StagesScreen(
    vm: FastingViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = "Metabolic stages",
                leading = {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(IntermIcons.Back, contentDescription = "Back", tint = c.ink2)
                    }
                },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.stages) { stage ->
                    val idx = state.stages.indexOf(stage)
                    StageCard(stage = stage, index = idx, currentIdx = state.stageIdx)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StageCard(stage: Stage, index: Int, currentIdx: Int) {
    val c = IntermTheme.colors
    val current = index == currentIdx
    val passed = index < currentIdx

    val borderColor = if (current) c.primary else c.border
    val borderWidth = if (current) 1.5.dp else 1.dp
    val cardBg = if (current) c.primarySoft else c.card
    val bodyColor = if (current) c.ink else c.ink2

    val (numBg, numFg) = when {
        current -> c.primary to c.surface
        passed -> c.ink to c.surface
        else -> c.border to c.muted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(numBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        color = numFg,
                        style = IntermTheme.typography.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.W600),
                    )
                }
                Text(
                    stage.name,
                    style = IntermTheme.typography.headerTitle.copy(fontSize = 16.sp),
                    color = c.ink,
                )
            }
            Text(stage.range, style = IntermTheme.typography.caption, color = c.muted)
        }
        Spacer(Modifier.height(10.dp))
        Text(stage.body, style = IntermTheme.typography.body, color = bodyColor)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stage.benefits.forEach { benefit ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (current) Color.Black.copy(alpha = 0.04f) else c.border2)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(benefit, style = IntermTheme.typography.caption.copy(fontSize = 11.sp), color = c.ink2)
                }
            }
        }
        if (current) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(c.primary))
                Text("You are here", style = IntermTheme.typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.W500), color = c.primary)
            }
        }
    }
}
