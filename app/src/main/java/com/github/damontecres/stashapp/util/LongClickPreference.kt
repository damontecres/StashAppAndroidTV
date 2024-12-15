package com.github.damontecres.stashapp.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class LongClickPreference(
    context: Context,
    attrs: AttributeSet?,
) : Preference(context, attrs) {
    var longClickListener: View.OnLongClickListener? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.setOnLongClickListener {
            longClickListener?.onLongClick(it) ?: false
        }
    }

    fun setOnLongClickListener(longClickListener: View.OnLongClickListener?) {
        this.longClickListener = longClickListener
    }
}
