package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.UpdateChecker
import kotlinx.coroutines.launch

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {
    private val fragment = MainFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, fragment)
                .commitNow()
        }
        maybeShowUpdate()
    }

    private fun maybeShowUpdate() {
        val checkForUpdates =
            PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("autoCheckForUpdates", true)
        if (checkForUpdates) {
            lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                UpdateChecker.checkForUpdate(this@MainActivity, false)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!fragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return fragment.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return fragment.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return fragment.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyMultiple(
        keyCode: Int,
        repeatCount: Int,
        event: KeyEvent,
    ): Boolean {
        return fragment.onKeyMultiple(keyCode, repeatCount, event) ||
            super.onKeyMultiple(
                keyCode,
                repeatCount,
                event,
            )
    }
}
