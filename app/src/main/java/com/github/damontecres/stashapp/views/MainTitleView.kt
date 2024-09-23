package com.github.damontecres.stashapp.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.TitleViewAdapter
import androidx.lifecycle.ViewModelProvider
import com.github.damontecres.stashapp.FilterListActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SettingsActivity
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.setup.ManageServersFragment
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.putFilterArgs
import com.github.damontecres.stashapp.views.models.ServerViewModel

class MainTitleView(context: Context, attrs: AttributeSet) :
    RelativeLayout(context, attrs),
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
            override fun getSearchAffordanceView(): View {
                return searchButton
            }
        }

    private val defaultFilters = mutableMapOf<DataType, FilterArgs>()

    init {
        val onFocusChangeListener = StashOnFocusChangeListener(context)

        val root = LayoutInflater.from(context).inflate(R.layout.title, this)
        iconButton = root.findViewById(R.id.icon)
        iconButton.setOnClickListener {
            GuidedStepSupportFragment.add(
                findFragment<Fragment>().parentFragmentManager,
                ManageServersFragment(),
            )
        }
        iconButton.onFocusChangeListener = onFocusChangeListener

        searchButton = root.findViewById(R.id.search_button)
        mPreferencesView = root.findViewById(R.id.settings_button)
        mPreferencesView.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(context, intent, null)
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

        refreshMenuItems()
    }

    override fun getTitleViewAdapter(): TitleViewAdapter {
        return mTitleViewAdapter
    }

    private fun getMenuItems(): Set<String> {
        val server = StashServer.getCurrentStashServer()
        if (server != null) {
            return server.serverPreferences.preferences.getStringSet(
                ServerPreferences.PREF_INTERFACE_MENU_ITEMS,
                ServerPreferences.DEFAULT_MENU_ITEMS,
            )!!
        }
        return setOf()
    }

    fun refreshMenuItems() {
        val menuItems = getMenuItems()

        fun getVis(key: String): Int {
            return if (key in menuItems) {
                View.VISIBLE
            } else {
                View.GONE
            }
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

    private inner class ClickListener(private val dataType: DataType) : OnClickListener {
        override fun onClick(v: View) {
            val serverPrefs = serverViewModel.currentServer.value!!.serverPreferences
            val filter = serverPrefs.defaultFilters[dataType]
            if (filter != null) {
                val intent =
                    Intent(v.context, FilterListActivity::class.java)
                        .putFilterArgs(FilterListActivity.INTENT_FILTER_ARGS, filter)
                startActivity(v.context, intent, null)
            } else {
                Log.w(TAG, "ServerPreferences.defaultFilters is missing $dataType")
                Toast.makeText(
                    v.context,
                    "Default filter not found for $dataType",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    companion object {
        private const val TAG = "MainTitleView"
    }
}
