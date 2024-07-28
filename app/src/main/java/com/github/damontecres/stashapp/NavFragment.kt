package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment

/**
 * Wraps a fragment for navigation
 */
class NavFragment(private val fragment: Fragment) : NavHostFragment() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction()
            .add(view.id, fragment)
            .commitNow()
    }
}
