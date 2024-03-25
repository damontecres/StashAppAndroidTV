package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import android.view.View.OnLongClickListener
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.appcompat.widget.ListPopupWindow
import com.github.damontecres.stashapp.R

/**
 * An OnLongClickListener which shows a popup of predefined options
 */
class PopupOnLongClickListener(
    private val popupOptions: List<String>,
    private val popUpWidth: Int = ListPopupWindow.WRAP_CONTENT,
    private val popupItemClickListener: AdapterView.OnItemClickListener,
) : OnLongClickListener {
    override fun onLongClick(view: View): Boolean {
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
                getMaxWidth(view.context, adapter)
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

    private fun getMaxWidth(
        context: Context,
        adapter: ArrayAdapter<String>,
    ): Int {
        if (adapter.viewTypeCount != 1) {
            throw IllegalStateException("Adapter creates more than 1 type of view")
        }

        val tempParent = FrameLayout(context)
        var maxWidth = 0
        var itemView: View? = null
        val measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        for (i in 0 until adapter.count) {
            if (adapter.getItemViewType(i) != Adapter.IGNORE_ITEM_VIEW_TYPE) {
                itemView = adapter.getView(i, itemView, tempParent)
                itemView.measure(measureSpec, measureSpec)
                if (itemView.measuredWidth > maxWidth) {
                    maxWidth = itemView.measuredWidth
                }
            }
        }
        return maxWidth
    }
}
