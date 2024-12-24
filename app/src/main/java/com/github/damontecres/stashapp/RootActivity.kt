package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationListener
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.views.models.ServerViewModel

class RootActivity :
    FragmentActivity(R.layout.activity_main),
    NavigationListener {
    private val serverViewModel: ServerViewModel by viewModels<ServerViewModel>()

    private lateinit var navigationManager: NavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serverViewModel.refresh()
        navigationManager = NavigationManager(this)
        navigationManager.addListener(this)

        serverViewModel.navigationManager = navigationManager

        if (savedInstanceState == null) {
            navigationManager.navigate(Destination.Main)
        } else {
            val previousDest = savedInstanceState.getDestination<Destination>()
            TODO()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // TODO
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
