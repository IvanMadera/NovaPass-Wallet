package com.example.novapass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.novapass.navigation.NovaPassNavGraph
import com.example.novapass.ui.theme.NovaPassTheme
import androidx.compose.ui.graphics.toArgb
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)

        val navBarColor = com.example.novapass.ui.theme.NovaColors.NavBar.toArgb()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(navBarColor)
        )

        // Warning 2 fix: SDK_INT siempre >= 31 en minSdk moderno, el bloque es redundante
        window.isNavigationBarContrastEnforced = false

        setContent {
            NovaPassTheme {
                NovaPassNavGraph()
            }
        }
    }
}
