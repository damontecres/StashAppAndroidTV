package com.github.damontecres.stashapp.navigation

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.github.damontecres.stashapp.GalleryFragment
import com.github.damontecres.stashapp.GroupFragment
import com.github.damontecres.stashapp.MainFragment
import com.github.damontecres.stashapp.MarkerDetailsFragment
import com.github.damontecres.stashapp.PerformerFragment
import com.github.damontecres.stashapp.PinFragment
import com.github.damontecres.stashapp.SceneDetailsFragment
import com.github.damontecres.stashapp.StashGridFragment
import com.github.damontecres.stashapp.StashSearchFragment
import com.github.damontecres.stashapp.StudioFragment
import com.github.damontecres.stashapp.TagFragment
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.playback.PlaybackSceneFragment
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.putDestination

class NavigationManager(
    activity: FragmentActivity,
) {
    private val fragmentManager = activity.supportFragmentManager
    private val onBackPressedDispatcher = activity.onBackPressedDispatcher
    private val listeners = mutableListOf<NavigationListener>()
    private val onBackPressedCallback: OnBackPressedCallback

    private val destinationStack = ArrayDeque<Destination>()

    init {
        onBackPressedCallback =
            onBackPressedDispatcher.addCallback(activity, false) {
                if (fragmentManager.backStackEntryCount > 0) {
                    // Prevent backing out from PIN
                    if (destinationStack.last() != Destination.Pin) {
                        destinationStack.removeLast()
                        fragmentManager.popBackStack()
                        listeners.forEach { it.onNavigate(destinationStack.last()) }
                    }
                }
            }
    }

    fun navigate(destination: Destination) {
        if (destination == Destination.Pin &&
            destinationStack.lastOrNull() == Destination.Pin
        ) {
            Log.v(TAG, "Ignore navigate to ${Destination.Pin}")
            return
        }
        val fragment =
            when (destination) {
                Destination.Main -> MainFragment()
                Destination.Search -> StashSearchFragment()
                Destination.Settings -> TODO()
                Destination.Pin -> PinFragment()

                is Destination.Item -> {
                    when (destination.dataType) {
                        DataType.SCENE -> SceneDetailsFragment()
                        DataType.TAG -> TagFragment()
                        DataType.GROUP -> GroupFragment()
                        DataType.PERFORMER -> PerformerFragment()
                        DataType.STUDIO -> StudioFragment()
                        DataType.GALLERY -> GalleryFragment()
                        DataType.MARKER -> MarkerDetailsFragment()
                        DataType.IMAGE -> TODO()
                    }
                }

                is Destination.Filter -> StashGridFragment(destination.filterArgs, null, destination.scrollToNextPage)

                is Destination.Playback -> PlaybackSceneFragment()
                is Destination.Playlist -> TODO()
            }
        val args = Bundle().putDestination(destination)
        fragment.arguments = args

        destinationStack.addLast(destination)
        listeners.forEach { it.onNavigate(destination) }
        fragmentManager.commit {
            addToBackStack(destination.toString())
            // TODO animation
            replace(android.R.id.content, fragment)
        }
        onBackPressedCallback.isEnabled = fragmentManager.backStackEntryCount > 0
    }

    fun clearPinFragment() {
        Log.v(TAG, "clearPinFragment")
        if (destinationStack.lastOrNull() == Destination.Pin) {
            destinationStack.removeLast()
            fragmentManager.popBackStack()
            if (destinationStack.isNotEmpty()) {
                listeners.forEach { it.onNavigate(destinationStack.last()) }
                onBackPressedCallback.isEnabled = fragmentManager.backStackEntryCount > 0
            } else {
                navigate(Destination.Main)
            }
        }
    }

    fun addListener(listener: NavigationListener) {
        listeners.add(listener)
    }

    fun saveInstanceState(bundle: Bundle) {
        bundle.putInt("count", destinationStack.size)
        destinationStack.forEachIndexed { index, destination ->
            bundle.putDestination("destination_$index", destination)
        }
    }

    fun restoreInstanceState(bundle: Bundle) {
        for (index in 0..<bundle.getInt("count")) {
            destinationStack.addLast(bundle.getDestination("destination_$index"))
        }
    }

    interface NavigationListener {
        fun onNavigate(destination: Destination)
    }

    companion object {
        const val DESTINATION_ARG = "destination"
        private const val TAG = "NavigationManager"
    }
}
