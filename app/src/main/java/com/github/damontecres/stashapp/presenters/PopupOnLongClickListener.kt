package com.github.damontecres.stashapp.presenters

import android.view.View
import android.view.View.OnLongClickListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth

/**
 * An OnLongClickListener which shows a popup of predefined options
 */
class PopupOnLongClickListener(
    private val popupOptions: List<String>,
    private val popUpWidth: Int = ListPopupWindow.WRAP_CONTENT,
    private val popupItemClickListener: AdapterView.OnItemClickListener,
) : OnLongClickListener {
    override fun onLongClick(view: View): Boolean {
        if (popupOptions.isEmpty()) {
            return true
        }
        val listPopUp =
            ListPopupWindow(
                view.context,
                null,
                android.R.attr.listPopupWindowStyle,
            )
        val adapter =
            ArrayAdapter(
                view.context,
                R.layout.popup_item,
                popupOptions,
            )

        listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
        listPopUp.anchorView = view
        listPopUp.width =
            if (popUpWidth == ListPopupWindow.WRAP_CONTENT) {
                getMaxMeasuredWidth(view.context, adapter)
            } else {
                popUpWidth
            }
        listPopUp.isModal = true

        listPopUp.setAdapter(adapter)

        listPopUp.setOnItemClickListener { parent: AdapterView<*>, v: View, position: Int, id: Long ->
            popupItemClickListener.onItemClick(parent, v, position, id)
            listPopUp.dismiss()
        }

        listPopUp.show()

        return true
    }
}
