package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.util.preferences
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val navigationManager: NavigationManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences by this@MainActivity.preferences.data.collectAsState(null)
            preferences?.let {
            }
        }
    }
}
