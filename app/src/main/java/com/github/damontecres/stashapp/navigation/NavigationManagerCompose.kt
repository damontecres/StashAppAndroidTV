package com.github.damontecres.stashapp.navigation

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.PinFragment
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.RootActivity
import com.github.damontecres.stashapp.SettingsFragment
import com.github.damontecres.stashapp.UpdateAppFragment
import com.github.damontecres.stashapp.setup.readonly.SettingsPinEntryFragment
import com.github.damontecres.stashapp.ui.NavDrawerFragment
import com.github.damontecres.stashapp.util.putDestination
import com.github.damontecres.stashapp.views.models.ServerViewModel
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popAll
import dev.olshevski.navigation.reimagined.popUpTo

class NavigationManagerCompose(
    activity: RootActivity,
    val serverViewModel: ServerViewModel,
) : NavigationManagerParent(activity) {
    private val navDrawerFragment: NavDrawerFragment

    init {
        val navFragment = fragmentManager.findFragmentByTag(NAV_FRAGMENT_TAG)
        if (navFragment is NavDrawerFragment) {
            navDrawerFragment = navFragment
        } else {
            navDrawerFragment = NavDrawerFragment()
        }
    }

    val controller: NavController<Destination>?
        get() = navDrawerFragment.navController

    private fun navigate(
        destination: Destination,
        popUpToMain: Boolean,
    ) {
        if (controller != null) {
            serverViewModel.command.value = null
            if (popUpToMain) {
                controller!!.popUpTo { it == Destination.Main }
            }
            if (destination != Destination.Main) {
                controller!!.navigate(destination)
            }
        } else {
            serverViewModel.submit(destination, popUpToMain)
        }
    }

    override fun navigate(destination: Destination) {
        val current = getCurrentFragment()
        if (destination == Destination.Pin) {
            // Enable so that backing out of the fragment will close the app
            onBackPressedCallback.isEnabled = true
        }
        if (destination == Destination.Pin && current is PinFragment) {
            if (DEBUG) Log.v(TAG, "Ignore navigate to ${Destination.Pin}")
            return
        }

        if (DEBUG) Log.v(TAG, "navigate: ${destination.fragmentTag}")
        val fragment =
            when (destination) {
                is Destination.Settings -> SettingsFragment()
                Destination.Pin -> PinFragment()
                Destination.SettingsPin -> SettingsPinEntryFragment()
//                Destination.Setup -> SetupFragment()

                is Destination.UpdateApp -> UpdateAppFragment()
//                is Destination.ManageServers -> ManageServersFragment()
//                is Destination.CreateFilter -> CreateFilterFragment()
//                is Destination.UpdateMarker -> MarkerPickerFragment()
                is Destination.Fragment -> {
                    fragmentManager.fragmentFactory.instantiate(
                        activity.classLoader,
                        destination.className,
                    )
                }

                else -> {
                    if (getCurrentFragment() != navDrawerFragment) {
                        Log.v(TAG, "getCurrentFragment() != navDrawerFragment")
                        setFragment(destination, navDrawerFragment)
                    }
                    when (destination) {
                        Destination.Main -> navigate(Destination.Main, true)
//                        is Destination.Filter -> controller.navigate(destination)
                        else -> navigate(destination, false)
                    }
                    return
                }
            }
        setFragment(destination, fragment)
    }

    private fun setFragment(
        destination: Destination,
        fragment: Fragment,
    ) {
        Log.v(TAG, "setFragment: destination=$destination")
        fragment.arguments = Bundle().putDestination(destination)
        val tag =
            if (fragment is NavDrawerFragment) {
                NAV_FRAGMENT_TAG
            } else {
                destination.fragmentTag
            }
        if (fragment is GuidedStepSupportFragment) {
            GuidedStepSupportFragment.add(fragmentManager, fragment, R.id.root_fragment)
        } else {
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
                replace(R.id.root_fragment, fragment, tag)
            }
        }
    }

    /**
     * Same as [navigate] except if the destination is [Destination.Filter], the backstack will reset to just the main page
     */
    fun navigateFromNavDrawer(destination: Destination) {
        if (DEBUG) Log.v(TAG, "navigate: ${destination.fragmentTag}")
        val current = getCurrentFragment()
        if (current != navDrawerFragment) {
            throw IllegalStateException(
                "navigateFromNavDrawer called when current fragment is not navDrawerFragment: $current",
            )
        }
        when (destination) {
            is Destination.Settings,
            Destination.Pin,
            Destination.SettingsPin,
            Destination.Setup,
            is Destination.UpdateApp,
            is Destination.ManageServers,
            is Destination.CreateFilter,
            is Destination.UpdateMarker,
            is Destination.Fragment,
            -> navigate(destination)

            Destination.Main,
            is Destination.Filter,
            -> navigate(destination, true)

            else -> navigate(destination, false)
        }
    }

    /**
     * End the current fragment and go to the previous one
     */
    override fun goBack() {
        if (getCurrentFragment() == navDrawerFragment) {
            controller?.pop()
        } else {
            fragmentManager.popBackStack()
        }
    }

    /**
     * Drop all of the back stack and go to the main page, reloading it
     */
    override fun goToMain() {
        Log.v(TAG, "goToMain")
        // Hacky
        while (fragmentManager.popBackStackImmediate()) {
        }
        //        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        if (getCurrentFragment() != navDrawerFragment) {
            Log.v(TAG, "goToMain: getCurrentFragment() != navDrawerFragment")
            setFragment(Destination.Main, navDrawerFragment)
        }
        controller?.popAll()
        controller?.navigate(Destination.Main)
//        navigate(Destination.Main)
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
//            activity.rootFragmentView.visibility = View.GONE
            notifyListeners(previousDestination, Destination.Main, null)
        }
        onBackPressedCallback.isEnabled = false
    }

    private fun getCurrentFragment(): Fragment? = fragmentManager.findFragmentById(R.id.root_fragment)

    companion object {
        private const val TAG = "NavigationManagerCompose"
        private const val NAV_FRAGMENT_TAG = "NavDrawerFragment_Tag"
        private const val DEBUG = false
    }
}
