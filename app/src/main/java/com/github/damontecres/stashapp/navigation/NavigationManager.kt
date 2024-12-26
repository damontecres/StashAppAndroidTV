package com.github.damontecres.stashapp.navigation

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.FilterFragment
import com.github.damontecres.stashapp.GalleryFragment
import com.github.damontecres.stashapp.GroupFragment
import com.github.damontecres.stashapp.MainFragment
import com.github.damontecres.stashapp.MarkerDetailsFragment
import com.github.damontecres.stashapp.PerformerFragment
import com.github.damontecres.stashapp.PinFragment
import com.github.damontecres.stashapp.RootActivity
import com.github.damontecres.stashapp.SceneDetailsFragment
import com.github.damontecres.stashapp.SearchForFragment
import com.github.damontecres.stashapp.SettingsFragment
import com.github.damontecres.stashapp.StashSearchFragment
import com.github.damontecres.stashapp.StudioFragment
import com.github.damontecres.stashapp.TagFragment
import com.github.damontecres.stashapp.UpdateAppFragment
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.image.ImageFragment
import com.github.damontecres.stashapp.playback.PlaybackSceneFragment
import com.github.damontecres.stashapp.playback.PlaylistMarkersFragment
import com.github.damontecres.stashapp.playback.PlaylistScenesFragment
import com.github.damontecres.stashapp.setup.ManageServersFragment
import com.github.damontecres.stashapp.setup.readonly.SettingsPinEntryFragment
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.putDestination
import com.github.damontecres.stashapp.views.MarkerPickerFragment

class NavigationManager(
    private val activity: RootActivity,
) {
    private val fragmentManager = activity.supportFragmentManager
    private val onBackPressedDispatcher = activity.onBackPressedDispatcher
    private val listeners = mutableListOf<NavigationListener>()
    private val onBackPressedCallback: OnBackPressedCallback

    private val destinationStack = ArrayDeque<Destination>()

    init {
        onBackPressedCallback =
            onBackPressedDispatcher.addCallback(activity, true) {
                Log.v(
                    TAG,
                    "fragmentManager.backStackEntryCount=${fragmentManager.backStackEntryCount}, " +
                        "destinationStack.lastOrNull()=${destinationStack.lastOrNull()}",
                )
                if (fragmentManager.backStackEntryCount > 0) {
                    if (destinationStack.last() == Destination.Main) {
                        activity.finish()
                    } else if (destinationStack.last() != Destination.Pin) {
                        // Prevent backing out from PIN, but not settings pin
                        destinationStack.removeLast()
                        fragmentManager.popBackStack()
                        val fragment =
                            fragmentManager.findFragmentByTag(
                                destinationStack.lastOrNull()?.toString(),
                            )
                        if (fragment != null) {
                            Log.v(TAG, "back: fragment=$fragment")
                            notifyListeners(destinationStack.last(), fragment)
                        }
                    }
                } else {
                    activity.finish()
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
                Destination.Settings -> SettingsFragment()
                Destination.Pin -> PinFragment()
                Destination.SettingsPin -> SettingsPinEntryFragment()

                is Destination.UpdateApp -> UpdateAppFragment()
                is Destination.ManageServers -> ManageServersFragment()

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

                is Destination.MarkerDetails -> MarkerDetailsFragment()

                is Destination.Slideshow -> ImageFragment()

                is Destination.Filter -> FilterFragment()

                is Destination.SearchFor -> SearchForFragment()
                is Destination.UpdateMarker -> MarkerPickerFragment()

                is Destination.Playback -> PlaybackSceneFragment()
                is Destination.Playlist -> {
                    when (destination.filterArgs.dataType) {
                        DataType.SCENE -> PlaylistScenesFragment()
                        DataType.MARKER -> PlaylistMarkersFragment()
                        else -> throw IllegalArgumentException("Playlist for ${destination.filterArgs.dataType} not supported")
                    }
                }

                is Destination.Fragment -> {
                    fragmentManager.fragmentFactory.instantiate(
                        activity.classLoader,
                        destination.className,
                    )
                }
            }
        val args = Bundle().putDestination(destination)
        fragment.arguments = args
        if (fragment is GuidedStepSupportFragment) {
            GuidedStepSupportFragment.add(fragmentManager, fragment, android.R.id.content)
        } else {
            fragmentManager.commit {
                addToBackStack(destination.toString())
                // TODO animation
                replace(android.R.id.content, fragment, destination.toString())
            }
        }
        Log.v(TAG, "next: fragment=$fragment")
        destinationStack.addLast(destination)
        notifyListeners(destination, fragment)
    }

    fun goBack() {
        onBackPressedCallback.handleOnBackPressed()
    }

    fun goToMain() {
        fragmentManager.popBackStack(Destination.Main.toString(), 0)
        destinationStack.removeAll { true }
        destinationStack.addLast(Destination.Main)
        val fragment =
            fragmentManager.findFragmentByTag(
                destinationStack.last().toString(),
            )!!
        notifyListeners(destinationStack.last(), fragment)
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
                notifyListeners(destinationStack.last(), fragment)
                onBackPressedCallback.isEnabled = fragmentManager.backStackEntryCount > 0
            } else if (destinationStack.lastOrNull() == Destination.SettingsPin) {
                destinationStack.removeLast()
                fragmentManager.popBackStack()
                navigate(Destination.Settings)
            } else {
                navigate(Destination.Main)
            }
        }
    }

    fun addListener(listener: NavigationListener) {
        listeners.add(listener)
    }

    private fun notifyListeners(
        destination: Destination,
        fragment: Fragment,
    ) {
        listeners.forEach { it.onNavigate(destination, fragment) }
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

    private fun Destination?.isPin() = this != null && (this == Destination.Pin || this == Destination.SettingsPin)

    companion object {
        const val DESTINATION_ARG = "destination"
        private const val TAG = "NavigationManager"
    }
}
