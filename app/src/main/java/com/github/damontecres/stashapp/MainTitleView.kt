package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.leanback.widget.TitleViewAdapter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.ServerPreferences

class MainTitleView(context: Context, attrs: AttributeSet) :
    RelativeLayout(context, attrs),
    TitleViewAdapter.Provider {
    private var mPreferencesView: ImageButton
    private lateinit var searchButton: ImageButton

    private val scenesButton: Button
    private val performersButton: Button
    private val studiosButton: Button
    private val tagsButton: Button
    private val moviesButton: Button
    private val markersButton: Button
    private val imagesButton: Button
    private val galleriesButton: Button

    private val mTitleViewAdapter =
        object : TitleViewAdapter() {
            override fun getSearchAffordanceView(): View {
                return searchButton
            }
        }

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.title, this)
        searchButton = root.findViewById(R.id.search_button)
        mPreferencesView = root.findViewById(R.id.settings_button)
        mPreferencesView.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(context, intent, null)
        }
        val onFocusChangeListener = StashOnFocusChangeListener(context)
        searchButton.onFocusChangeListener = onFocusChangeListener
        mPreferencesView.onFocusChangeListener = onFocusChangeListener

        scenesButton = root.findViewById<Button>(R.id.scenes_button)
        scenesButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.SCENE.name)
            startActivity(context, intent, null)
        }
        scenesButton.onFocusChangeListener = onFocusChangeListener

        imagesButton = root.findViewById<Button>(R.id.images_button)
        imagesButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.IMAGE.name)
            startActivity(context, intent, null)
        }
        imagesButton.onFocusChangeListener = onFocusChangeListener

        performersButton = root.findViewById<Button>(R.id.performers_button)
        performersButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.PERFORMER.name)
            startActivity(context, intent, null)
        }
        performersButton.onFocusChangeListener = onFocusChangeListener

        studiosButton = root.findViewById<Button>(R.id.studios_button)
        studiosButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.STUDIO.name)
            startActivity(context, intent, null)
        }
        studiosButton.onFocusChangeListener = onFocusChangeListener

        tagsButton = root.findViewById<Button>(R.id.tags_button)
        tagsButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.TAG.name)
            startActivity(context, intent, null)
        }
        tagsButton.onFocusChangeListener = onFocusChangeListener

        moviesButton = root.findViewById<Button>(R.id.movies_button)
        moviesButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.MOVIE.name)
            startActivity(context, intent, null)
        }
        moviesButton.onFocusChangeListener = onFocusChangeListener

        markersButton = root.findViewById<Button>(R.id.markers_button)
        markersButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.MARKER.name)
            startActivity(context, intent, null)
        }
        markersButton.onFocusChangeListener = onFocusChangeListener

        galleriesButton = root.findViewById<Button>(R.id.galleries_button)
        galleriesButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.GALLERY.name)
            startActivity(context, intent, null)
        }
        galleriesButton.onFocusChangeListener = onFocusChangeListener

        refreshMenuItems()
    }

    override fun getTitleViewAdapter(): TitleViewAdapter {
        return mTitleViewAdapter
    }

    fun refreshMenuItems() {
        val menuItems =
            ServerPreferences(context).preferences.getStringSet(
                ServerPreferences.PREF_INTERFACE_MENU_ITEMS,
                ServerPreferences.DEFAULT_MENU_ITEMS,
            )!!

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
        moviesButton.visibility = getVis("movies")
        markersButton.visibility = getVis("markers")
        galleriesButton.visibility = getVis("galleries")
    }
}
