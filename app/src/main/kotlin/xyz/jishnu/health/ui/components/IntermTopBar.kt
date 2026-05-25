package xyz.jishnu.health.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun IntermTopBar(
    title: String,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.CenterStart) {
            leading?.invoke()
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = IntermTheme.typography.headerTitle,
                color = IntermTheme.colors.ink,
            )
        }
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.CenterEnd) {
            trailing?.invoke()
        }
    }
}
