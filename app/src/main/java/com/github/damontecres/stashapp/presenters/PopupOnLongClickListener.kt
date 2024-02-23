package com.github.damontecres.stashapp.presenters

import android.view.View
import android.view.View.OnLongClickListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import com.github.damontecres.stashapp.R

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
        listPopUp.width = ListPopupWindow.WRAP_CONTENT
        listPopUp.isModal = true

        val adapter =
            ArrayAdapter(
                view.context,
                R.layout.popup_item,
                popupOptions,
            )
        listPopUp.setAdapter(adapter)

        listPopUp.setOnItemClickListener { parent: AdapterView<*>, v: View, position: Int, id: Long ->
            popupItemClickListener.onItemClick(parent, v, position, id)
            listPopUp.dismiss()
        }

        listPopUp.show()

        return true
    }
}
