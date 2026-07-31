package id.djawadwipa.manajemenkontrakan.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.djawadwipa.manajemenkontrakan.ui.screens.AppLockScreen
import id.djawadwipa.manajemenkontrakan.ui.theme.ManajemenKontrakanTheme

@Composable
fun ManajemenKontrakanRoot(
    onExit: () -> Unit,
) {
    val mainViewModel: MainViewModel = viewModel()
    val state by mainViewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val lockConfigured = settings.lockEnabled &&
        settings.pinSalt.isNotBlank() &&
        settings.pinHash.isNotBlank()

    var sessionUnlocked by rememberSaveable {
        mutableStateOf(false)
    }
    var backgroundAt by rememberSaveable {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(lockConfigured, settings.pinHash) {
        sessionUnlocked = !lockConfigured
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(
        lifecycleOwner,
        lockConfigured,
        settings.autoLockMinutes,
    ) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundAt = SystemClock.elapsedRealtime()
                }

                Lifecycle.Event.ON_START -> {
                    if (lockConfigured && backgroundAt > 0L) {
                        val timeoutMillis = settings.autoLockMinutes
                            .coerceAtLeast(0)
                            .toLong() * 60_000L
                        val elapsed = SystemClock.elapsedRealtime() - backgroundAt
                        if (elapsed >= timeoutMillis) {
                            sessionUnlocked = false
                        }
                    }
                    backgroundAt = 0L
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ManajemenKontrakanTheme(themeMode = settings.themeMode) {
        when {
            state.isLoading -> {
                Surface(modifier = Modifier.fillMaxSize()) {}
            }

            lockConfigured && !sessionUnlocked -> {
                AppLockScreen(
                    settings = settings,
                    onUnlocked = { sessionUnlocked = true },
                )
            }

            else -> {
                ManajemenKontrakanApp(
                    viewModel = mainViewModel,
                    onLockNow = { sessionUnlocked = false },
                    onExit = onExit,
                )
            }
        }
    }
}
