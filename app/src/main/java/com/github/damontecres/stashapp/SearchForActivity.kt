package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Search for something
 */
class SearchForActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, SearchForFragment()).commitNow()
        }
    }
}
