package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.FragmentActivity
import com.github.damontecres.stashapp.setup.SetupActivity
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.UpdateChecker

/**
 * Require a PIN code to view the app
 *
 * If a PIN is not set, just delegates directly to [MainActivity]
 */
class PinActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            val currentServer = StashServer.getCurrentStashServer(this)
            if (currentServer == null) {
                val intent = Intent(this, SetupActivity::class.java)
                startActivity(intent)
            } else {
                onBackPressedDispatcher.addCallback(this) {
                    // Finish this activity and everything above (typically another Activity if the the app was resumed)
                    finishAffinity()
                }
                if (intent.getBooleanExtra(UpdateBroadcastReceiver.INTENT_APP_UPDATED, false)) {
                    val installedVersion = UpdateChecker.getInstalledVersion(this)
                    Toast
                        .makeText(this, "App updated to $installedVersion!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentServer = StashServer.getCurrentStashServer(this)
        if (currentServer != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_browse_fragment, PinFragment())
                .commitNow()
        }
    }

    companion object {
        private const val TAG = "PinActivity"
    }
}
