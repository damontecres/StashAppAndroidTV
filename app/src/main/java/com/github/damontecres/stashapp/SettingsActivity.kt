package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.setup.readonly.ReadOnlyPinEntryFragment

class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.v(
            "SettingsActivity",
            "onCreate: savedInstanceState==null: ${savedInstanceState == null}",
        )
        if (savedInstanceState == null) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            if (preferences.getBoolean(getString(R.string.pref_key_read_only_mode), false)) {
                GuidedStepSupportFragment.addAsRoot(
                    this,
                    ReadOnlyPinEntryFragment {
                        supportFragmentManager.commitNow {
                            replace(R.id.main_browse_fragment, SettingsFragment())
                        }
                    },
                    R.id.main_browse_fragment,
                )
            } else {
                supportFragmentManager.commitNow {
                    replace(R.id.main_browse_fragment, SettingsFragment())
                }
            }
        }
    }
}
