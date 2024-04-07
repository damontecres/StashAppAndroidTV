package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.Toast
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

            val checkForUpdates =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("autoCheckForUpdates", true)
            if (checkForUpdates) {
                lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    UpdateChecker.checkForUpdate(this@MainActivity, false)
                }
            }
            if (intent.getBooleanExtra(UpdateBroadcastReceiver.INTENT_APP_UPDATED, false)) {
                val installedVersion = UpdateChecker.getInstalledVersion(this)
                Toast.makeText(this, "App updated to $installedVersion!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        if (!fragment.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
