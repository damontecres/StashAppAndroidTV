package com.github.damontecres.stashapp.navigation

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.github.damontecres.stashapp.GalleryFragment
import com.github.damontecres.stashapp.GroupFragment
import com.github.damontecres.stashapp.MainFragment
import com.github.damontecres.stashapp.MarkerDetailsFragment
import com.github.damontecres.stashapp.PerformerFragment
import com.github.damontecres.stashapp.SceneDetailsFragment
import com.github.damontecres.stashapp.StashGridFragment
import com.github.damontecres.stashapp.StashSearchFragment
import com.github.damontecres.stashapp.StudioFragment
import com.github.damontecres.stashapp.TagFragment
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.putDestination
import kotlinx.serialization.Serializable
import java.util.Stack

class NavigationManager(
    activity: FragmentActivity,
) {
    private val fragmentManager = activity.supportFragmentManager
    private val onBackPressedDispatcher = activity.onBackPressedDispatcher
    private val listeners = mutableListOf<NavigationListener>()
    private val onBackPressedCallback: OnBackPressedCallback

    private val destinationStack = Stack<Destination>()

    init {
        onBackPressedCallback =
            onBackPressedDispatcher.addCallback(activity, false) {
                if (fragmentManager.backStackEntryCount > 0) {
                    destinationStack.pop()
                    fragmentManager.popBackStack()
                    listeners.forEach { it.onNavigate(destinationStack.peek()) }
                }
            }
//        fragmentManager.addOnBackStackChangedListener {
//            val newFragment = fragmentManager.findFragmentById(android.R.id.content)
//            Log.v(TAG, "BackStackChangedListener: newFragment=$newFragment")
//            newFragment?.let {
//                val dest = newFragment.arguments?.getDestination<Destination>()
//                Log.v(TAG, "BackStackChangedListener: dest=$dest")
//                if (dest != null) {
//                    listeners.forEach { it.onNavigate(dest) }
//                }
//            }
//        }
    }

    fun navigate(destination: Destination) {
        val fragment =
            when (destination) {
                Destination.Main -> MainFragment()
                Destination.Search -> StashSearchFragment()
                Destination.Settings -> TODO()

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

                is Destination.Playback -> TODO()
                is Destination.Playlist -> TODO()
            }
        val args = Bundle().putDestination(destination)
        fragment.arguments = args

        destinationStack.push(destination)
        listeners.forEach { it.onNavigate(destination) }
        fragmentManager.commit {
            addToBackStack(destination.toString())
            // TODO animation
            replace(android.R.id.content, fragment)
        }
        onBackPressedCallback.isEnabled = fragmentManager.backStackEntryCount > 0
    }

    fun addListener(listener: NavigationListener) {
        listeners.add(listener)
    }

    companion object {
        const val DESTINATION_ARG = "destination"
        private const val TAG = "NavigationManager"
    }
}

interface NavigationListener {
    fun onNavigate(destination: Destination)
}

@Serializable
sealed interface Destination {
    @Serializable
    data object Main : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object Search : Destination

    @Serializable
    data class Item(
        val dataType: DataType,
        val id: String,
    ) : Destination

    @Serializable
    data class Playback(
        val sceneId: String,
        val position: Long,
    ) : Destination

    @Serializable
    data class Filter(
        val filterArgs: FilterArgs,
        val scrollToNextPage: Boolean,
    ) : Destination

    @Serializable
    data class Playlist(
        val filterArgs: FilterArgs,
        val position: Int,
    ) : Destination

    companion object {
        fun fromStashData(item: StashData): Item = Item(getDataType(item), item.id)

        fun getDataType(item: StashData): DataType =
            when (item) {
                is SlimSceneData, is FullSceneData, is VideoSceneData -> DataType.SCENE
                is PerformerData -> DataType.PERFORMER
                else -> TODO()
            }
    }
}

class NavigationOnItemViewClickedListener(
    private val navigationManager: NavigationManager,
) : OnItemViewClickedListener {
    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        when (item) {
            is StashData -> navigationManager.navigate(Destination.fromStashData(item))
            else -> TODO()
        }
    }
}
