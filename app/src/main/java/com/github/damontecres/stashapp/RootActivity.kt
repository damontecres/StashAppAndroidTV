package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.util.KeyEventDispatcher
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class RootActivity :
    FragmentActivity(R.layout.activity_main),
    NavigationManager.NavigationListener {
    private val serverViewModel: ServerViewModel by viewModels<ServerViewModel>()
    private lateinit var navigationManager: NavigationManager
    private var appHasPin by Delegates.notNull<Boolean>()
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(TAG, "onCreate: savedInstanceState==null:${savedInstanceState == null}")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        appHasPin =
            PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("pinCode", "")
                .isNotNullOrBlank()

        navigationManager = NavigationManager(this)
        navigationManager.addListener(this)
        StashApplication.navigationManager = navigationManager

        serverViewModel.navigationManager = navigationManager

        if (serverViewModel.refresh()) {
            if (savedInstanceState == null) {
                if (appHasPin) {
                    navigationManager.navigate(Destination.Pin)
                } else {
                    navigationManager.navigate(Destination.Main)
                }
            } else {
//                navigationManager.restoreInstanceState(savedInstanceState)
                if (appHasPin) {
                    navigationManager.navigate(Destination.Pin)
                }
            }

            maybeShowUpdate()
        } else {
            // No server configured
            navigationManager.navigate(Destination.Setup)
        }
    }

    private fun maybeShowUpdate() {
        val checkForUpdates =
            PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("autoCheckForUpdates", true)
        if (checkForUpdates) {
            lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                UpdateChecker.checkForUpdate(this@RootActivity, false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (appHasPin) {
            navigationManager.navigate(Destination.Pin)
        }
    }

    override fun onNavigate(
        destination: Destination,
        fragment: Fragment,
    ) {
        Log.v(TAG, "onNavigate: dest=${destination.fragmentTag}")
        currentFragment = fragment
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val fragment = currentFragment
        if (fragment is KeyEventDispatcher) {
            return fragment.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        val fragment = currentFragment
        return if (fragment != null && fragment is KeyEvent.Callback) {
            fragment.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        val fragment = currentFragment
        return if (fragment != null && fragment is KeyEvent.Callback) {
            fragment.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)
        } else {
            super.onKeyUp(keyCode, event)
        }
    }

    override fun onKeyLongPress(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        val fragment = currentFragment
        return if (fragment != null && fragment is KeyEvent.Callback) {
            fragment.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event)
        } else {
            super.onKeyLongPress(keyCode, event)
        }
    }

    override fun onKeyMultiple(
        keyCode: Int,
        repeatCount: Int,
        event: KeyEvent?,
    ): Boolean {
        val fragment = currentFragment
        return if (fragment != null && fragment is KeyEvent.Callback) {
            fragment.onKeyMultiple(keyCode, repeatCount, event) ||
                super.onKeyMultiple(
                    keyCode,
                    repeatCount,
                    event,
                )
        } else {
            super.onKeyMultiple(keyCode, repeatCount, event)
        }
    }

    companion object {
        private const val TAG = "RootActivity"
    }
}
