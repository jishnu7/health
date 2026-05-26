package xyz.jishnu.health

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.ui.nav.IntermNavHost
import xyz.jishnu.health.ui.nav.Routes
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.FastingViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepo: SettingsRepository

    private val fastingVm: FastingViewModel by viewModels()
    private val pendingDeepLink = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingDeepLink.value = intent.extractDeepLinkRoute()
        setContent {
            IntermTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(IntermTheme.colors.bg),
                    color = IntermTheme.colors.bg,
                ) {
                    var startRoute by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(Unit) {
                        val s = settingsRepo.settings.first()
                        startRoute = if (s.onboarded) Routes.Home else Routes.OnboardWelcome
                    }
                    val deepLinkRoute by pendingDeepLink.collectAsState()
                    startRoute?.let {
                        IntermNavHost(
                            vm = fastingVm,
                            startDestination = it,
                            deepLinkRoute = deepLinkRoute,
                            onDeepLinkConsumed = { pendingDeepLink.value = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.extractDeepLinkRoute()?.let { pendingDeepLink.value = it }
    }

    companion object {
        const val EXTRA_OPEN_ROUTE = "xyz.jishnu.health.extra.OPEN_ROUTE"
    }
}

private fun Intent.extractDeepLinkRoute(): String? =
    getStringExtra(MainActivity.EXTRA_OPEN_ROUTE)?.takeIf { it.isNotBlank() }
