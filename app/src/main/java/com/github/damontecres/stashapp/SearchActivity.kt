package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.github.damontecres.stashapp.util.Constants

class NavFragment() : NavHostFragment() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction()
            .add(view.id, StashSearchFragment())
            .commitNow()
    }
}

class SearchActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            if (intent.getBooleanExtra(Constants.USE_NAV_CONTROLLER, false)) {
                val navFragment = NavFragment()
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
