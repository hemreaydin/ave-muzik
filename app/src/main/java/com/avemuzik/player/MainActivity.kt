package com.avemuzik.player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avemuzik.player.model.ThemeMode
import com.avemuzik.player.ui.AveMuzikRoot
import com.avemuzik.player.ui.permission.AudioPermission
import com.avemuzik.player.ui.theme.AveMuzikTheme
import com.avemuzik.player.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        val container = (application as AveMuzikApplication).container
        val settings = container.settingsRepository
        // Splash yalnızca tema tercihi diskten okunana kadar (birkaç ms) kalır.
        splash.setKeepOnScreenCondition { settings.themeMode.value == null }

        lifecycle.addObserver(container.playerController)

        // İzin varsa tarama, arayüz çizilirken arka planda başlasın.
        if (AudioPermission.isGranted(this)) container.musicRepository.ensureLoaded()

        enableEdgeToEdge()
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val darkTheme = isDarkTheme(themeMode ?: ThemeMode.Default)

            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            AveMuzikTheme(darkTheme = darkTheme) {
                AveMuzikRoot(container)
            }
        }
    }
}
