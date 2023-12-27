package com.github.damontecres.stashapp


import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.SearchOrbView
import androidx.leanback.widget.TitleViewAdapter
import com.github.damontecres.stashapp.data.sceneFromSlimSceneData


class MainTitleView : RelativeLayout, TitleViewAdapter.Provider {

    private var mPreferencesView: ImageButton
    private lateinit var mSearchOrbView: SearchOrbView
    private lateinit var mTitleView: TextView

    val mTitleViewAdapter = object:TitleViewAdapter(){
        override fun getSearchAffordanceView(): View {
            return mSearchOrbView
        }
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val root = LayoutInflater.from(context).inflate(R.layout.title, this)
        mTitleView = root.findViewById<TextView>(R.id.title_text) as TextView
        mSearchOrbView = root.findViewById<SearchOrbView>(R.id.search_orb)
        mPreferencesView = root.findViewById<ImageButton>(R.id.settings_button)
        mPreferencesView.setOnClickListener(object:View.OnClickListener{
            override fun onClick(v: View?) {
                val intent = Intent(context, SettingsActivity::class.java)
                startActivity(context, intent, null)
            }

        })
    }

    override fun getTitleViewAdapter(): TitleViewAdapter {
        return mTitleViewAdapter
    }


}