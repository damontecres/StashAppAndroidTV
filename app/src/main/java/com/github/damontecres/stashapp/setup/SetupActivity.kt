package com.github.damontecres.stashapp.setup

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.PinActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.getCurrentStashServer

class SetupActivity : FragmentActivity(R.layout.frame_layout) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(
                this,
                ConfigureServerStep(true),
                R.id.frame_fragment,
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing && getCurrentStashServer(this) != null) {
            val intent = Intent(this, PinActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        const val ACTION_SERVER_URL = 1L
        const val ACTION_SERVER_API_KEY = 2L
    }
}
