package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.core.view.WindowCompat
import androidx.tv.material3.Surface
import com.github.damontecres.stashapp.ui.App
import com.github.damontecres.stashapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppTheme {
                Surface(
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    App()
                }
            }
        }
    }
}
