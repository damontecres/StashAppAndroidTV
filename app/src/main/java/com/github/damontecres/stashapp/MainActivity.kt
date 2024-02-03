package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.activity.addCallback

/**
 * Loads [MainFragment].
 */
class MainActivity() : SecureFragmentActivity() {
    private val fragment = MainFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, fragment)
                .commitNow()
        }
        onBackPressedDispatcher.addCallback(this) {
            // Prevents going back to the PinActivity, there's probably a better way
            finish()
        }
    }

    override fun onBackPressed() {
        if (!fragment.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
