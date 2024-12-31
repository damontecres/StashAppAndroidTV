package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.util.KeyEventDispatcher
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlin.properties.Delegates

/**
 * The only activity in the app
 */
class RootActivity :
    FragmentActivity(R.layout.activity_root),
    NavigationManager.NavigationListener {
    private val serverViewModel: ServerViewModel by viewModels<ServerViewModel>()
    private lateinit var navigationManager: NavigationManager
    private var appHasPin by Delegates.notNull<Boolean>()
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setUpLifeCycleListeners()

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

        val currentServer = StashServer.findConfiguredStashServer(StashApplication.getApplication())
        if (currentServer != null) {
            serverViewModel.init(currentServer)
            serverViewModel.currentServer.observe(this) { server ->
                if (server != null) {
                    if (savedInstanceState == null) {
                        if (!appHasPin) {
                            serverViewModel.currentServer.removeObservers(this@RootActivity)
                            navigationManager.goToMain()
                        }
                    }
                }
            }
        } else {
            // No server configured
            navigationManager.navigate(Destination.Setup)
        }
    }

    override fun onResume() {
        super.onResume()
        if (appHasPin) {
            navigationManager.navigate(Destination.Pin)
        }
    }

    override fun onNavigate(
        previousDestination: Destination?,
        nextDestination: Destination,
        fragment: Fragment,
    ) {
        Log.v(
            TAG,
            "onNavigate: $previousDestination=>$nextDestination",
        )
        currentFragment = fragment
    }

    // Delegate key events to the current fragment

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

    private fun setUpLifeCycleListeners() {
        if (DEBUG) {
            supportFragmentManager.registerFragmentLifecycleCallbacks(
                object :
                    FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentAttached(
                        fm: FragmentManager,
                        f: Fragment,
                        context: Context,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentAttached: f=${f::class.simpleName} (${f.tag})",
                        )
                    }

                    override fun onFragmentCreated(
                        fm: FragmentManager,
                        f: Fragment,
                        savedInstanceState: Bundle?,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentCreated: f=${f::class.simpleName} (${f.tag})",
                        )
                    }

                    override fun onFragmentViewCreated(
                        fm: FragmentManager,
                        f: Fragment,
                        v: View,
                        savedInstanceState: Bundle?,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentViewCreated: f=${f::class.simpleName} (${f.tag})",
                        )
                    }

                    override fun onFragmentStarted(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentStarted: f=${f::class.simpleName} (${f.tag})",
                        )
                    }

                    override fun onFragmentResumed(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentResumed: f=${f::class.simpleName} (${f.tag})",
                        )
                    }

                    override fun onFragmentPaused(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentPaused: f=${f::class.simpleName} (${f.tag})",
                        )
                    }

                    override fun onFragmentStopped(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentStopped: f=${f::class.simpleName} (${f.tag})",
                        )
                    }

                    override fun onFragmentSaveInstanceState(
                        fm: FragmentManager,
                        f: Fragment,
                        outState: Bundle,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentSaveInstanceState: f=${f::class.simpleName} (${f.tag})",
                        )
                    }

                    override fun onFragmentViewDestroyed(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentViewDestroyed: f=${f::class.simpleName} (${f.tag})",
                        )
                    }

                    override fun onFragmentDestroyed(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        Log.v(
                            TAG_LIFECYCLE,
                            "onFragmentDestroyed: f=${f::class.simpleName} (${f.tag})",
                        )
                    }
                },
                false,
            )
        }
    }

    companion object {
        private const val TAG = "RootActivity"
        private const val TAG_LIFECYCLE = "LifecycleTracking"

        private const val DEBUG = false
    }
}
