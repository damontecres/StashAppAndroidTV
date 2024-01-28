package com.github.damontecres.stashapp

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.leanback.widget.TitleViewAdapter

class ListTitleView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs), TitleViewAdapter.Provider {
    private lateinit var filterButton: Button

    val mTitleViewAdapter =
        object : TitleViewAdapter() {
            override fun getSearchAffordanceView(): View {
                return filterButton
            }
        }

    override fun getTitleViewAdapter(): TitleViewAdapter {
        return mTitleViewAdapter
    }

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.list_title_view, this)
        filterButton = root.findViewById(R.id.filter_button)
        filterButton.setOnClickListener {
            Toast.makeText(context, "Filter button clicked!", Toast.LENGTH_SHORT).show()
        }
        filterButton.setOnFocusChangeListener { view: View, focused: Boolean ->
            if (focused) {
                filterButton.setBackgroundColor(context.getColor(R.color.selected_background))
            } else {
                filterButton.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }
}
