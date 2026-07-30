package id.djawadwipa.manajemenkontrakan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import id.djawadwipa.manajemenkontrakan.ui.ManajemenKontrakanApp
import id.djawadwipa.manajemenkontrakan.ui.MainViewModel
import id.djawadwipa.manajemenkontrakan.ui.theme.ManajemenKontrakanTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            ManajemenKontrakanTheme {
                val mainViewModel: MainViewModel = viewModel()
                ManajemenKontrakanApp(mainViewModel, onExit = { finishAffinity() })
            }
        }
    }
}
