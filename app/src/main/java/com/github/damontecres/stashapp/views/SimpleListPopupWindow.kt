package com.github.damontecres.stashapp.views

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupWindow
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
    ListPopupWindowBuilder(anchorView, options, callback).build().show()
}

class ListPopupWindowBuilder(
    private val anchorView: View,
    private val options: List<String>,
    private val itemClickListener: (position: Int) -> Unit,
) {
    private var showListener: (() -> Unit)? = null
    private var dismissListener: PopupWindow.OnDismissListener? = null

    fun onShowListener(callback: (() -> Unit)): ListPopupWindowBuilder {
        this.showListener = callback
        return this
    }

    fun onDismissListener(callback: PopupWindow.OnDismissListener): ListPopupWindowBuilder {
        this.dismissListener = callback
        return this
    }

    fun build(): ListPopupWindow {
        val context = anchorView.context
        val listPopUp =
            object : ListPopupWindow(
                context,
                null,
                android.R.attr.listPopupWindowStyle,
            ) {
                override fun show() {
                    super.show()
                    showListener?.invoke()
                    listView?.requestFocus()
                }
            }

        listPopUp.setOnDismissListener(dismissListener)
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
            itemClickListener(position)
        }
        return listPopUp
    }
}
