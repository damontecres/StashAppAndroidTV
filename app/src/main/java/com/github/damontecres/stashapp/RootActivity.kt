package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.ViewPumpAppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationListener
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.navigation.NavigationManagerLeanback
import com.github.damontecres.stashapp.util.KeyEventDispatcher
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.composeEnabled
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.maybeGetDestination
import com.github.damontecres.stashapp.util.putDestination
import com.github.damontecres.stashapp.views.models.ServerViewModel
import dev.b3nedikt.restring.Restring
import kotlin.properties.Delegates

/**
 * The only activity in the app
 */
class RootActivity :
    AppCompatActivity(),
    NavigationListener {
    private val serverViewModel by viewModels<ServerViewModel>()
    private var useCompose by Delegates.notNull<Boolean>()
    private lateinit var navigationManager: NavigationManager
    private var currentFragment: Fragment? = null
    lateinit var rootFragmentView: View

    private var hasCheckedForUpdate = false
    private lateinit var loadingView: ContentLoadingProgressBar
    private lateinit var bgLogo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useCompose = composeEnabled(this)
        navigationManager =
            if (useCompose) {
                NavigationManagerCompose(this, serverViewModel)
            } else {
                NavigationManagerLeanback(this)
            }
        navigationManager.addListener(this)
        StashApplication.navigationManager = navigationManager
        serverViewModel.navigationManager = navigationManager

        if (useCompose) {
            setContentView(R.layout.activity_root_compose)
        } else {
            setContentView(R.layout.activity_root)
        }
        rootFragmentView = findViewById(R.id.root_fragment)

        setUpLifeCycleListeners()
        Log.v(
            TAG,
            "onCreate: savedInstanceState==null:${savedInstanceState == null}, currentFragment==null:${currentFragment == null}",
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        val appHasPin = appHasPin()

        if (savedInstanceState != null) {
            navigationManager.previousDestination = savedInstanceState.maybeGetDestination()
            Log.d(TAG, "Restoring destination: ${navigationManager.previousDestination}")
        }

        loadingView = findViewById(R.id.loading_progress_bar)
        bgLogo = findViewById(R.id.background_logo)

        val currentServer = StashServer.findConfiguredStashServer(StashApplication.getApplication())
        if (currentServer != null) {
            Log.i(TAG, "Server configured")
            StashServer.setCurrentStashServer(StashApplication.getApplication(), currentServer)
            serverViewModel.init(currentServer)

            serverViewModel.serverConnection.observe(this) { result ->
                loadingView.hide()
                when (result) {
                    is ServerViewModel.ServerConnection.Failure -> {
                        Log.w(TAG, "Exception connecting to server", result.exception)
                        Toast
                            .makeText(
                                this,
                                "Error connecting to ${result.server.url}",
                                Toast.LENGTH_LONG,
                            ).show()
                        if (!appHasPin) {
                            navigationManager.navigate(Destination.ManageServers(true))
                        }
                    }

                    ServerViewModel.ServerConnection.NotConfigured -> {
                        Log.i(TAG, "No servers configured, starting setup")
                        navigationManager.navigate(Destination.Setup)
                    }

                    ServerViewModel.ServerConnection.Pending -> {}
                    ServerViewModel.ServerConnection.Success -> {}
                }
            }

            serverViewModel.currentServer.observe(this) { server ->
                if (server != null) {
                    if (savedInstanceState == null) {
                        if (!appHasPin) {
                            serverViewModel.currentServer.removeObservers(this@RootActivity)
                            if (useCompose) {
                                // Workaround because compose goToMain is special
                                navigationManager.navigate(Destination.Main)
                            } else {
                                navigationManager.goToMain()
                            }
                        }
                    }
                }
            }

            serverViewModel.destination.observe(this) { destination ->
                val windowInsetsController =
                    WindowCompat.getInsetsController(window, window.decorView)
                if (destination is Destination.Playback ||
                    destination is Destination.Playlist ||
                    destination is Destination.Slideshow ||
                    destination is Destination.UpdateMarker
                ) {
                    Log.v(TAG, "Hiding system bars for $destination")
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    Log.v(TAG, "Showing system bars for $destination")
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        } else {
            loadingView.hide()
            Log.i(TAG, "No server, starting setup")
            // No server configured
            navigationManager.navigate(Destination.Setup)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")
        hasCheckedForUpdate = false
        if (appHasPin()) {
            navigationManager.navigate(Destination.Pin)
        } else {
            loadingView.hide()
            bgLogo.animateToInvisible(View.GONE)
            serverViewModel.updateServerPreferences()
        }
    }

    override fun onPause() {
        Log.v(TAG, "onPause")
        if (appHasPin()) {
            navigationManager.navigate(Destination.Pin)
        }
        super.onPause()
    }

    @OptIn(UnstableApi::class)
    override fun onStop() {
        Log.v(TAG, "onStop")
        StashExoPlayer.releasePlayer()
        super.onStop()
    }

    override fun onNavigate(
        previousDestination: Destination?,
        nextDestination: Destination,
        fragment: Fragment?,
    ) {
        serverViewModel.setCurrentDestination(nextDestination)
        loadingView.hide()
        bgLogo.animateToInvisible(View.GONE)
        Log.v(
            TAG,
            "onNavigate: $previousDestination=>$nextDestination",
        )
        currentFragment = fragment
        if (nextDestination == Destination.Main && !hasCheckedForUpdate) {
            serverViewModel.maybeShowUpdate(this)
            hasCheckedForUpdate = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.v(TAG, "onSaveInstanceState")
        super.onSaveInstanceState(outState)
        val dest = navigationManager.previousDestination
        if (dest != null) {
            outState.putDestination(dest)
        } else {
            outState.putParcelable(NavigationManager.DESTINATION_ARG, null)
        }
    }

    private fun appHasPin(): Boolean =
        PreferenceManager
            .getDefaultSharedPreferences(this)
            .getString("pinCode", "")
            .isNotNullOrBlank()

    // Delegate key events to the current fragment

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val fragment = currentFragment
        if (fragment is KeyEventDispatcher && fragment.isAdded) {
            return fragment.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        val fragment = currentFragment
        return if (fragment != null && fragment.isAdded && fragment is KeyEvent.Callback) {
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
        return if (fragment != null && fragment.isAdded && fragment is KeyEvent.Callback) {
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
        return if (fragment != null && fragment.isAdded && fragment is KeyEvent.Callback) {
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
        return if (fragment != null && fragment.isAdded && fragment is KeyEvent.Callback) {
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

    private val appCompatDelegate: AppCompatDelegate by lazy {
        ViewPumpAppCompatDelegate(
            baseDelegate = super.getDelegate(),
            baseContext = this,
            wrapContext = Restring::wrapContext,
        )
    }

    override fun getDelegate(): AppCompatDelegate = appCompatDelegate

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
