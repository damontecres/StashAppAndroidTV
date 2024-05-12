package com.github.damontecres.stashapp.setup

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.R
import kotlin.properties.Delegates

class SetupActivity : FragmentActivity(R.layout.frame_layout) {
    private var firstTimeSetup by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firstTimeSetup = intent.getBooleanExtra(INTENT_SETUP_FIRST_TIME, false)

        GuidedStepSupportFragment.addAsRoot(this, ConfigureServerStep(firstTimeSetup), R.id.frame_fragment)
    }

    companion object {
        val INTENT_SETUP_FIRST_TIME = SetupActivity::class.java.name + ".firstTimeSetup"

        const val ACTION_SERVER_URL = 1L
        const val ACTION_SERVER_API_KEY = 2L
    }
}
