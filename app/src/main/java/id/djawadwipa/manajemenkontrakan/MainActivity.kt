package id.djawadwipa.manajemenkontrakan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import id.djawadwipa.manajemenkontrakan.ui.ManajemenKontrakanRoot

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            ManajemenKontrakanRoot(
                onExit = { finishAffinity() },
            )
        }
    }
}
