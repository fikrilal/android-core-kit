package dev.fikril.androidcorekit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.fikril.androidcorekit.core.designsystem.theme.AndroidCoreKitTheme
import dev.fikril.androidcorekit.navigation.AndroidCoreKitRoot

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidCoreKitTheme {
                AndroidCoreKitRoot()
            }
        }
    }
}
