package com.github.damontecres.stashapp.navigation

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.FilterFragment
import com.github.damontecres.stashapp.GalleryFragment
import com.github.damontecres.stashapp.GroupFragment
import com.github.damontecres.stashapp.MainFragment
import com.github.damontecres.stashapp.MarkerDetailsFragment
import com.github.damontecres.stashapp.PerformerFragment
import com.github.damontecres.stashapp.PinFragment
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.RootActivity
import com.github.damontecres.stashapp.SceneDetailsFragment
import com.github.damontecres.stashapp.SearchForFragment
import com.github.damontecres.stashapp.SettingsFragment
import com.github.damontecres.stashapp.StashSearchFragment
import com.github.damontecres.stashapp.StudioFragment
import com.github.damontecres.stashapp.TagFragment
import com.github.damontecres.stashapp.UpdateAppFragment
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterFragment
import com.github.damontecres.stashapp.image.ImageFragment
import com.github.damontecres.stashapp.playback.PlaybackSceneFragment
import com.github.damontecres.stashapp.playback.PlaylistMarkersFragment
import com.github.damontecres.stashapp.playback.PlaylistScenesFragment
import com.github.damontecres.stashapp.setup.ManageServersFragment
import com.github.damontecres.stashapp.setup.SetupFragment
import com.github.damontecres.stashapp.setup.readonly.SettingsPinEntryFragment
import com.github.damontecres.stashapp.util.maybeGetDestination
import com.github.damontecres.stashapp.util.putDestination
import com.github.damontecres.stashapp.views.MarkerPickerFragment

/**
 * Manages navigating to pages in the app
 */
class NavigationManager(
    private val activity: RootActivity,
) {
    private val fragmentManager = activity.supportFragmentManager
    private val listeners = mutableListOf<NavigationListener>()

    /**
     * A [OnBackPressedCallback] that always finishes the [RootActivity]
     */
    private val onBackPressedCallback: OnBackPressedCallback =
        activity.onBackPressedDispatcher.addCallback(activity, false) {
            activity.finish()
        }

    init {
        fragmentManager.addOnBackStackChangedListener {
            val current = fragmentManager.findFragmentById(R.id.root_fragment)
            val dest = current?.arguments?.maybeGetDestination<Destination>()
            if (DEBUG) Log.v(TAG, "backStackChanged: current=$current, dest=${dest?.fragmentTag}")
            if (dest != null) {
                notifyListeners(dest, current)
            }
        }
    }

    private val slideAnimDestinations =
        setOf(
            Destination.Settings,
            Destination.SettingsPin,
            Destination.ManageServers,
        )

    fun navigate(destination: Destination) {
        if (DEBUG) Log.v(TAG, "navigate: ${destination.fragmentTag}")
        val current = getCurrentFragment()
        if (destination == Destination.Pin && current is PinFragment) {
            if (DEBUG) Log.v(TAG, "Ignore navigate to ${Destination.Pin}")
            return
        }
        if (destination == Destination.Pin) {
            // Enable so that backing out of the fragment will close the app
            onBackPressedCallback.isEnabled = true
        }
        val fragment =
            when (destination) {
                Destination.Main -> MainFragment()
                Destination.Search -> StashSearchFragment()
                Destination.Settings -> SettingsFragment()
                Destination.Pin -> PinFragment()
                Destination.SettingsPin -> SettingsPinEntryFragment()
                Destination.Setup -> SetupFragment()

                is Destination.UpdateApp -> UpdateAppFragment()
                is Destination.ManageServers -> ManageServersFragment()
                is Destination.CreateFilter -> CreateFilterFragment()

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

        fragment.arguments = Bundle().putDestination(destination)

        if (fragment is GuidedStepSupportFragment) {
            GuidedStepSupportFragment.add(fragmentManager, fragment, R.id.root_fragment)
        } else {
            if (DEBUG) Log.v(TAG, "Setting ${destination.fragmentTag}: $fragment")
            fragmentManager.commit {
                if (destination != Destination.Main) {
                    addToBackStack(destination.fragmentTag)
                }
                if (destination in slideAnimDestinations) {
                    setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                    )
                } else {
                    setCustomAnimations(
                        R.animator.fade_in,
                        R.animator.fade_out,
                        R.animator.fade_in,
                        R.animator.fade_out,
                    )
                }
                replace(R.id.root_fragment, fragment, destination.fragmentTag)
            }
        }
        notifyListeners(destination, fragment)
    }

    /**
     * End the current fragment and go to the previous one
     */
    fun goBack() {
        fragmentManager.popBackStack()
    }

    /**
     * Drop all of the back stack and go back to the main page
     */
    fun goToMain() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        navigate(Destination.Main)
    }

    /**
     * Remove the [PinFragment]
     */
    fun clearPinFragment() {
        if (getCurrentFragment() !is PinFragment) {
            throw IllegalStateException("Current fragment is not PinFragment")
        }
        fragmentManager.popBackStackImmediate()
        if (getCurrentFragment() == null) {
            navigate(Destination.Main)
        }
        onBackPressedCallback.isEnabled = false
    }

    fun addListener(listener: NavigationListener) {
        listeners.add(listener)
    }

    private fun getCurrentFragment(): Fragment? = fragmentManager.findFragmentById(R.id.root_fragment)

    private fun notifyListeners(
        destination: Destination,
        fragment: Fragment,
    ) {
        listeners.forEach { it.onNavigate(destination, fragment) }
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
        private const val DEBUG = false
    }
}
