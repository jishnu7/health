package xyz.jishnu.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import xyz.jishnu.health.ui.nav.IntermNavHost
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.FastingViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val fastingVm: FastingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            IntermTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(IntermTheme.colors.bg),
                    color = IntermTheme.colors.bg,
                ) {
                    IntermNavHost(vm = fastingVm)
                }
            }
        }
    }
}
