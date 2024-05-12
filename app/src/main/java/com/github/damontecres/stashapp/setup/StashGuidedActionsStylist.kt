package com.github.damontecres.stashapp.setup

import android.content.Context
import androidx.leanback.widget.GuidedAction
import androidx.leanback.widget.GuidedActionsStylist

class StashGuidedActionsStylist(private val context: Context) : GuidedActionsStylist() {
    override fun onBindViewHolder(
        vh: ViewHolder,
        action: GuidedAction,
    ) {
        super.onBindViewHolder(vh, action)
//        vh.itemView.setOnLongClickListener {
//            Toast.makeText(context, "Long clicked ${action.id}", Toast.LENGTH_LONG).show()
//            true
//        }
    }
}
