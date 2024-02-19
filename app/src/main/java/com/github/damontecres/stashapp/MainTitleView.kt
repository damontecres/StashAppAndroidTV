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

        val menuItems =
            ServerPreferences(context).preferences.getStringSet(
                ServerPreferences.PREF_INTERFACE_MENU_ITEMS,
                ServerPreferences.DEFAULT_MENU_ITEMS,
            )!!

        val scenesButton = root.findViewById<Button>(R.id.scenes_button)
        scenesButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.SCENE.name)
            startActivity(context, intent, null)
        }
        scenesButton.onFocusChangeListener = onFocusChangeListener
        if ("scenes" !in menuItems) {
            scenesButton.visibility = View.GONE
        }

        val performersButton = root.findViewById<Button>(R.id.performers_button)
        performersButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.PERFORMER.name)
            startActivity(context, intent, null)
        }
        performersButton.onFocusChangeListener = onFocusChangeListener
        if ("performers" !in menuItems) {
            performersButton.visibility = View.GONE
        }

        val studiosButton = root.findViewById<Button>(R.id.studios_button)
        studiosButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.STUDIO.name)
            startActivity(context, intent, null)
        }
        studiosButton.onFocusChangeListener = onFocusChangeListener
        if ("studios" !in menuItems) {
            studiosButton.visibility = View.GONE
        }

        val tagsButton = root.findViewById<Button>(R.id.tags_button)
        tagsButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.TAG.name)
            startActivity(context, intent, null)
        }
        tagsButton.onFocusChangeListener = onFocusChangeListener
        if ("tags" !in menuItems) {
            tagsButton.visibility = View.GONE
        }

        val moviesButton = root.findViewById<Button>(R.id.movies_button)
        moviesButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.MOVIE.name)
            startActivity(context, intent, null)
        }
        moviesButton.onFocusChangeListener = onFocusChangeListener
        if ("movies" !in menuItems) {
            moviesButton.visibility = View.GONE
        }

        val markersButton = root.findViewById<Button>(R.id.markers_button)
        markersButton.setOnClickListener {
            val intent = Intent(context, FilterListActivity::class.java)
            intent.putExtra("dataType", DataType.MARKER.name)
            startActivity(context, intent, null)
        }
        markersButton.onFocusChangeListener = onFocusChangeListener
        if ("markers" !in menuItems) {
            markersButton.visibility = View.GONE
        }
    }

    override fun getTitleViewAdapter(): TitleViewAdapter {
        return mTitleViewAdapter
    }
}
