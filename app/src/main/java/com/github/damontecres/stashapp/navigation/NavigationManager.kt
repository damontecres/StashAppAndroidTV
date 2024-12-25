package com.github.damontecres.stashapp.navigation

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.github.damontecres.stashapp.FilterFragment
import com.github.damontecres.stashapp.GalleryFragment
import com.github.damontecres.stashapp.GroupFragment
import com.github.damontecres.stashapp.MainFragment
import com.github.damontecres.stashapp.MarkerDetailsFragment
import com.github.damontecres.stashapp.PerformerFragment
import com.github.damontecres.stashapp.PinFragment
import com.github.damontecres.stashapp.SceneDetailsFragment
import com.github.damontecres.stashapp.SearchForFragment
import com.github.damontecres.stashapp.StashSearchFragment
import com.github.damontecres.stashapp.StudioFragment
import com.github.damontecres.stashapp.TagFragment
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.image.ImageFragment
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
                        val fragment =
                            fragmentManager.findFragmentByTag(
                                destinationStack.lastOrNull()?.toString(),
                            )!!
                        Log.v(TAG, "back: fragment=$fragment")
                        listeners.forEach { it.onNavigate(destinationStack.last(), fragment) }
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
                        DataType.IMAGE -> throw IllegalArgumentException("Image not supported here")
                    }
                }

                is Destination.Slideshow -> ImageFragment()

                is Destination.Filter -> FilterFragment()

                is Destination.SearchFor -> SearchForFragment()

                is Destination.Playback -> PlaybackSceneFragment()
                is Destination.Playlist -> TODO()
            }
        val args = Bundle().putDestination(destination)
        fragment.arguments = args

        fragmentManager.commit {
            addToBackStack(destination.toString())
            // TODO animation
            replace(android.R.id.content, fragment, destination.toString())
        }
        Log.v(TAG, "next: fragment=$fragment")
        destinationStack.addLast(destination)
        listeners.forEach { it.onNavigate(destination, fragment) }

        onBackPressedCallback.isEnabled = fragmentManager.backStackEntryCount > 0
    }

    fun goBack() {
        onBackPressedCallback.handleOnBackPressed()
    }

    fun clearPinFragment() {
        Log.v(TAG, "clearPinFragment")
        if (destinationStack.lastOrNull() == Destination.Pin) {
            destinationStack.removeLast()
            fragmentManager.popBackStack()
            if (destinationStack.isNotEmpty()) {
                val fragment =
                    fragmentManager.findFragmentByTag(
                        destinationStack.lastOrNull()?.toString(),
                    )!!
                listeners.forEach { it.onNavigate(destinationStack.last(), fragment) }
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
        fun onNavigate(
            destination: Destination,
            fragment: Fragment,
        )
    }

    companion object {
        const val DESTINATION_ARG = "destination"
        private const val TAG = "NavigationManager"
    }
}
