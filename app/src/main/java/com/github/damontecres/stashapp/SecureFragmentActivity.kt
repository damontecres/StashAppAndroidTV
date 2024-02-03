package com.github.damontecres.stashapp

import android.os.Bundle
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity

/**
 * Just a FragmentActivity which sets FLAG_SECURE
 */
open class SecureFragmentActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
    ) {
        super.onCreate(savedInstanceState, persistentState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }
}
