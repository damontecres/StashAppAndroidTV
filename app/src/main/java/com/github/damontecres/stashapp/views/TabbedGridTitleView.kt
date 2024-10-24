package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.TitleViewAdapter
import com.github.damontecres.stashapp.R

class TabbedGridTitleView(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs), TitleViewAdapter.Provider {
    override fun getTitleViewAdapter(): TitleViewAdapter {
        return object : TitleViewAdapter() {
            override fun getSearchAffordanceView(): View {
                return findViewById<Button>(R.id.sort_button)
            }
        }
    }
}
