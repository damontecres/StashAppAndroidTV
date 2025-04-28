package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.leanback.widget.TitleViewAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.views.models.ServerViewModel

/**
 * The top title bar which has buttons for each [DataType]
 *
 * Clicking them goes to the default filter for that [DataType]
 */
class MainTitleView(
    context: Context,
    attrs: AttributeSet,
) : RelativeLayout(context, attrs),
    TitleViewAdapter.Provider {
    private val serverViewModel by lazy {
        ViewModelProvider(findFragment<Fragment>().requireActivity())[ServerViewModel::class]
    }

    private var mPreferencesView: ImageButton
    private lateinit var searchButton: ImageButton

    private val iconButton: ImageButton
    private val scenesButton: Button
    private val performersButton: Button
    private val studiosButton: Button
    private val tagsButton: Button
    private val groupsButton: Button
    private val markersButton: Button
    private val imagesButton: Button
    private val galleriesButton: Button

    private val mTitleViewAdapter =
        object : TitleViewAdapter() {
            override fun getSearchAffordanceView(): View = searchButton
        }

    class FocusListener : View.OnFocusChangeListener {
        private val listeners = mutableListOf<View.OnFocusChangeListener>()

        override fun onFocusChange(
            v: View,
            hasFocus: Boolean,
        ) {
            listeners.forEach { it.onFocusChange(v, hasFocus) }
        }

        fun addListener(listener: View.OnFocusChangeListener) {
            listeners.add(listener)
        }
    }

    val focusListener = FocusListener()

    init {
        val onFocusChangeListener = focusListener
        focusListener.addListener(StashOnFocusChangeListener(context))

        val root = LayoutInflater.from(context).inflate(R.layout.title, this)
        iconButton = root.findViewById(R.id.icon)
        iconButton.setOnClickListener {
            serverViewModel.navigationManager.navigate(Destination.ManageServers(false))
        }
        iconButton.onFocusChangeListener = onFocusChangeListener

        searchButton = root.findViewById(R.id.search_button)
        mPreferencesView = root.findViewById(R.id.settings_button)
        mPreferencesView.setOnClickListener {
            val preferences = PreferenceManager.getDefaultSharedPreferences(root.context)
            val destination =
                if (preferences.getBoolean(
                        root.context.getString(R.string.pref_key_read_only_mode),
                        false,
                    )
                ) {
                    Destination.SettingsPin
                } else {
                    Destination.Settings(PreferenceScreenOption.BASIC)
                }
            serverViewModel.navigationManager.navigate(destination)
        }

        searchButton.onFocusChangeListener = onFocusChangeListener
        mPreferencesView.onFocusChangeListener = onFocusChangeListener

        scenesButton = root.findViewById(R.id.scenes_button)
        scenesButton.setOnClickListener(ClickListener(DataType.SCENE))
        scenesButton.onFocusChangeListener = onFocusChangeListener

        imagesButton = root.findViewById(R.id.images_button)
        imagesButton.setOnClickListener(ClickListener(DataType.IMAGE))
        imagesButton.onFocusChangeListener = onFocusChangeListener

        performersButton = root.findViewById(R.id.performers_button)
        performersButton.setOnClickListener(ClickListener(DataType.PERFORMER))
        performersButton.onFocusChangeListener = onFocusChangeListener

        studiosButton = root.findViewById(R.id.studios_button)
        studiosButton.setOnClickListener(ClickListener(DataType.STUDIO))
        studiosButton.onFocusChangeListener = onFocusChangeListener

        tagsButton = root.findViewById(R.id.tags_button)
        tagsButton.setOnClickListener(ClickListener(DataType.TAG))
        tagsButton.onFocusChangeListener = onFocusChangeListener

        groupsButton = root.findViewById(R.id.groups_button)
        groupsButton.setOnClickListener(ClickListener(DataType.GROUP))
        groupsButton.onFocusChangeListener = onFocusChangeListener

        markersButton = root.findViewById(R.id.markers_button)
        markersButton.setOnClickListener(ClickListener(DataType.MARKER))
        markersButton.onFocusChangeListener = onFocusChangeListener

        galleriesButton = root.findViewById(R.id.galleries_button)
        galleriesButton.setOnClickListener(ClickListener(DataType.GALLERY))
        galleriesButton.onFocusChangeListener = onFocusChangeListener
    }

    override fun getTitleViewAdapter(): TitleViewAdapter = mTitleViewAdapter

    fun refreshMenuItems(serverPreferences: ServerPreferences) {
        val menuItems =
            serverPreferences.preferences
                .getStringSet(
                    ServerPreferences.PREF_INTERFACE_MENU_ITEMS,
                    ServerPreferences.DEFAULT_MENU_ITEMS,
                )!!

        fun getVis(key: String): Int =
            if (key in menuItems) {
                View.VISIBLE
            } else {
                View.GONE
            }
        scenesButton.visibility = getVis("scenes")
        imagesButton.visibility = getVis("images")
        performersButton.visibility = getVis("performers")
        studiosButton.visibility = getVis("studios")
        tagsButton.visibility = getVis("tags")
        groupsButton.visibility =
            if ("groups" in menuItems || "movies" in menuItems) {
                View.VISIBLE
            } else {
                View.GONE
            }
        markersButton.visibility = getVis("markers")
        galleriesButton.visibility = getVis("galleries")
    }

    private inner class ClickListener(
        private val dataType: DataType,
    ) : OnClickListener {
        override fun onClick(v: View) {
            val serverPrefs = serverViewModel.currentServer.value!!.serverPreferences
            val filter = serverPrefs.defaultFilters[dataType] ?: FilterArgs(dataType)
            serverViewModel.navigationManager.navigate(Destination.Filter(filter, false))
        }
    }

    companion object {
        private const val TAG = "MainTitleView"
    }
}
