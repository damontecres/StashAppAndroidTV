package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlin.properties.Delegates

class RootActivity :
    FragmentActivity(R.layout.activity_main),
    NavigationManager.NavigationListener {
    private val serverViewModel: ServerViewModel by viewModels<ServerViewModel>()

    private lateinit var navigationManager: NavigationManager

    private var appHasPin by Delegates.notNull<Boolean>()

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

        serverViewModel.refresh()
        navigationManager = NavigationManager(this)
        navigationManager.addListener(this)

        serverViewModel.navigationManager = navigationManager

        if (savedInstanceState == null) {
            if (appHasPin) {
                navigationManager.navigate(Destination.Pin)
            } else {
                navigationManager.navigate(Destination.Main)
            }
        } else {
            navigationManager.restoreInstanceState(savedInstanceState)
            if (appHasPin) {
                navigationManager.navigate(Destination.Pin)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (appHasPin) {
            navigationManager.navigate(Destination.Pin)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.v(TAG, "onSaveInstanceState")
        navigationManager.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Log.v(TAG, "onRestoreInstanceState")
        navigationManager.restoreInstanceState(savedInstanceState)
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onNavigate(destination: Destination) {
//        val titleView = findViewById<MainTitleView>(R.id.browse_title_group)
        Log.v(TAG, "onNavigate: destination=$destination")
        if (destination == Destination.Main) {
            setTheme(R.style.Theme_StashAppAndroidTV)
//            titleView.animateToVisible()
        } else {
            setTheme(R.style.NoTitleTheme)
//            titleView.animateToInvisible(View.GONE)
        }
    }

    companion object {
        private const val TAG = "RootActivity"
    }
}
