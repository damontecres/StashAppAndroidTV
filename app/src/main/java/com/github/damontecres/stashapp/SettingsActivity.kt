package com.github.damontecres.stashapp

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import java.util.prefs.Preferences

class SettingsActivity() : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, SettingsFragment())
                    .commitNow()
        }
    }
}