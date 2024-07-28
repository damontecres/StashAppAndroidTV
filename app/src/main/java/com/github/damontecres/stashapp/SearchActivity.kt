package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.github.damontecres.stashapp.util.isNavHostActive

class SearchActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            if (isNavHostActive()) {
                val navFragment = NavFragment(StashSearchFragment())
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_browse_fragment, navFragment)
                    .commitNow()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_browse_fragment, StashSearchFragment())
                    .commitNow()
            }
        }
    }
}
