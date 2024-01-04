package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.fragment.app.FragmentActivity

/**
 * Loads [MainFragment].
 */
class MainActivity() : SecureFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
        onBackPressedDispatcher.addCallback(this) {
            // Prevents going back to the PinActivity, there's probably a better way
            finish()
        }

    }
}