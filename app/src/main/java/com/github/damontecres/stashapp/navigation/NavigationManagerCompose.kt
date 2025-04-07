package com.github.damontecres.stashapp.navigation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.PinFragment
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.RootActivity
import com.github.damontecres.stashapp.SettingsFragment
import com.github.damontecres.stashapp.UpdateAppFragment
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterFragment
import com.github.damontecres.stashapp.playback.PlaylistMarkersFragment
import com.github.damontecres.stashapp.playback.PlaylistScenesFragment
import com.github.damontecres.stashapp.setup.ManageServersFragment
import com.github.damontecres.stashapp.setup.SetupFragment
import com.github.damontecres.stashapp.setup.readonly.SettingsPinEntryFragment
import com.github.damontecres.stashapp.util.putDestination
import com.github.damontecres.stashapp.views.MarkerPickerFragment
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popUpTo

class NavigationManagerCompose(
    activity: RootActivity,
) : NavigationManagerParent(activity) {
    lateinit var controller: NavController<Destination>

    override fun navigate(destination: Destination) {
        if (DEBUG) Log.v(TAG, "navigate: ${destination.fragmentTag}")
        if (destination == Destination.Pin) {
            composeNavigate(destination)
        } else if (destination == Destination.Main) {
            controller.popUpTo { it == Destination.Main }
        } else if (destination is Destination.Filter) {
            controller.popUpTo { it == Destination.Main }
            controller.navigate(destination)
        } else {
            controller.navigate(destination)
        }
    }

    fun composeNavigate(destination: Destination) {
        val current = getCurrentFragment()
        if (destination == Destination.Pin && current is PinFragment) {
            if (DEBUG) Log.v(TAG, "Ignore navigate to ${Destination.Pin}")
            return
        }
        if (destination == Destination.Pin) {
            activity.rootFragmentView.visibility = View.VISIBLE
            // Enable so that backing out of the fragment will close the app
            onBackPressedCallback.isEnabled = true
        }
        val fragment =
            when (destination) {
                Destination.Main -> null // ComposeMainFragment()
                Destination.Search -> null // StashSearchFragment()
                is Destination.Settings -> SettingsFragment()
                Destination.Pin -> PinFragment()
                Destination.SettingsPin -> SettingsPinEntryFragment()
                Destination.Setup -> SetupFragment()

                is Destination.UpdateApp -> UpdateAppFragment()
                is Destination.ManageServers -> ManageServersFragment()
                is Destination.CreateFilter -> CreateFilterFragment()

                is Destination.Item -> {
                    when (destination.dataType) {
                        DataType.SCENE -> null
                        DataType.TAG -> null
                        DataType.GROUP -> null
                        DataType.PERFORMER -> null
                        DataType.STUDIO -> null
                        DataType.GALLERY -> null
                        DataType.MARKER -> null
                        DataType.IMAGE -> throw IllegalArgumentException("Image not supported here")
                    }
                }

                is Destination.MarkerDetails -> null // MarkerDetailsFragment()

                is Destination.Slideshow -> null // ImageFragment()

                is Destination.Filter -> null

                is Destination.SearchFor -> null
                is Destination.UpdateMarker -> MarkerPickerFragment()

                is Destination.Playback -> null // PlaybackSceneFragment()
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

        fragment?.arguments = Bundle().putDestination(destination)

        if (fragment is GuidedStepSupportFragment) {
            GuidedStepSupportFragment.add(fragmentManager, fragment, R.id.root_fragment)
        } else if (fragment != null) {
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
        notifyListeners(previousDestination, destination, fragment)
        previousDestination = destination
    }

    /**
     * End the current fragment and go to the previous one
     */
    override fun goBack() {
        fragmentManager.popBackStack()
    }

    /**
     * Drop all of the back stack and go back to the main page
     */
    override fun goToMain() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        navigate(Destination.Main)
    }

    /**
     * Remove the [PinFragment]
     */
    override fun clearPinFragment() {
        if (getCurrentFragment() !is PinFragment) {
            throw IllegalStateException("Current fragment is not PinFragment")
        }
        fragmentManager.popBackStackImmediate()
        if (getCurrentFragment() == null) {
            navigate(Destination.Main)
            activity.rootFragmentView.visibility = View.GONE
            notifyListeners(previousDestination, Destination.Main, null)
        }
        onBackPressedCallback.isEnabled = false
    }

    private fun getCurrentFragment(): Fragment? = fragmentManager.findFragmentById(R.id.root_fragment)

    companion object {
        const val DESTINATION_ARG = "destination"
        private const val TAG = "NavigationManager"
        private const val DEBUG = false
    }
}
