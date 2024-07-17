package com.github.damontecres.stashapp.views

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth

/**
 * Show a simple [ListPopupWindow] anchored to the specified view
 */
fun showSimpleListPopupWindow(
    anchorView: View,
    options: List<String>,
    callback: (position: Int) -> Unit,
) {
    val context = anchorView.context
    val listPopUp =
        ListPopupWindow(
            context,
            null,
            android.R.attr.listPopupWindowStyle,
        )
    val adapter =
        ArrayAdapter(
            context,
            R.layout.popup_item,
            options,
        )
    listPopUp.setAdapter(adapter)
    listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
    listPopUp.anchorView = anchorView

    listPopUp.width = getMaxMeasuredWidth(context, adapter)
    listPopUp.isModal = true

    listPopUp.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
        listPopUp.dismiss()
        callback(position)
    }
    listPopUp.show()
    listPopUp.listView?.requestFocus()
}
