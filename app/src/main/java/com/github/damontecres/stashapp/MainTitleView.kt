package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.leanback.widget.SearchOrbView
import androidx.leanback.widget.TitleViewAdapter

class MainTitleView : RelativeLayout, TitleViewAdapter.Provider {
    private var mPreferencesView: ImageButton
    private lateinit var mSearchOrbView: SearchOrbView

    val mTitleViewAdapter =
        object : TitleViewAdapter() {
            override fun getSearchAffordanceView(): View {
                return mSearchOrbView
            }
        }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val root = LayoutInflater.from(context).inflate(R.layout.title, this)
        mSearchOrbView = root.findViewById<SearchOrbView>(R.id.search_orb)
        mPreferencesView = root.findViewById<ImageButton>(R.id.settings_button)
        mPreferencesView.setOnClickListener(
            object : OnClickListener {
                override fun onClick(v: View?) {
                    val intent = Intent(context, SettingsActivity::class.java)
                    startActivity(context, intent, null)
                }
            },
        )

        val mFocusedZoom =
            context.resources.getFraction(
                androidx.leanback.R.fraction.lb_search_orb_focused_zoom,
                1,
                1,
            )
        val mScaleDurationMs =
            context.resources.getInteger(
                androidx.leanback.R.integer.lb_search_orb_scale_duration_ms,
            )

        val onFocusChangeListener =
            OnFocusChangeListener { v, hasFocus ->
                val zoom = if (hasFocus) mFocusedZoom else 1f
                v.animate().scaleX(zoom).scaleY(zoom).setDuration(mScaleDurationMs.toLong()).start()

                if (hasFocus) {
                    v.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_background))
                } else {
                    v.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.default_card_background,
                        ),
                    )
                }
            }
        mSearchOrbView.onFocusChangeListener = onFocusChangeListener
        mPreferencesView.onFocusChangeListener = onFocusChangeListener

        val scenesButton = root.findViewById<Button>(R.id.scenes_button)
        scenesButton.setOnClickListener {
            val intent = Intent(context, SceneListActivity::class.java)
            startActivity(context, intent, null)
        }
        scenesButton.onFocusChangeListener = onFocusChangeListener

        val performersButton = root.findViewById<Button>(R.id.performers_button)
        performersButton.setOnClickListener {
            val intent = Intent(context, PerformerListActivity::class.java)
            startActivity(context, intent, null)
        }
        performersButton.onFocusChangeListener = onFocusChangeListener

        val studiosButton = root.findViewById<Button>(R.id.studios_button)
        studiosButton.setOnClickListener {
            val intent = Intent(context, StudioListActivity::class.java)
            startActivity(context, intent, null)
        }
        studiosButton.onFocusChangeListener = onFocusChangeListener

        val tagsButton = root.findViewById<Button>(R.id.tags_button)
        tagsButton.setOnClickListener {
            val intent = Intent(context, TagListActivity::class.java)
            startActivity(context, intent, null)
        }
        tagsButton.onFocusChangeListener = onFocusChangeListener
    }

    override fun getTitleViewAdapter(): TitleViewAdapter {
        return mTitleViewAdapter
    }
}
