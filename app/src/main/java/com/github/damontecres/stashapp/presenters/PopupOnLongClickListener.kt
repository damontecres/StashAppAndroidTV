package com.github.damontecres.stashapp.presenters

import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import com.github.damontecres.stashapp.StashOnFocusChangeListener

/**
 * An OnLongClickListener which shows a popup of predefined options
 */
class PopupOnLongClickListener(
    private val popupOptions: List<String>,
    private val popupItemClickListener: AdapterView.OnItemClickListener,
) : OnLongClickListener {
    override fun onLongClick(view: View): Boolean {
        val listPopUp =
            ListPopupWindow(
                view.context,
                null,
                android.R.attr.listPopupWindowStyle,
            )
        listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
        listPopUp.anchorView = view
        // listPopUp.width = ViewGroup.LayoutParams.MATCH_PARENT
        // TODO: Better width calculation
        listPopUp.width = 200
        listPopUp.isModal = true

        val focusChangeListener = StashOnFocusChangeListener(view.context)

        val adapter =
            object : ArrayAdapter<String>(
                view.context,
                android.R.layout.simple_list_item_1,
                popupOptions,
            ) {
                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup,
                ): View {
                    val itemView = super.getView(position, convertView, parent)
                    // TODO: this doesn't seem to work?
                    itemView.onFocusChangeListener = focusChangeListener
                    return itemView
                }
            }
        listPopUp.setAdapter(adapter)

        listPopUp.setOnItemClickListener { parent: AdapterView<*>, v: View, position: Int, id: Long ->
            popupItemClickListener.onItemClick(parent, v, position, id)
            listPopUp.dismiss()
        }

        listPopUp.show()

        return true
    }
}
