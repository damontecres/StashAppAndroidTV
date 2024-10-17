package com.github.damontecres.stashapp.views

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.get
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.util.getRandomSort

/**
 * Manages a button for sorting items. It will setup the list popup with the right sort options for the given [DataType].
 */
class SortButtonManager(
    val serverVersion: Version,
    val newSortCallback: (SortAndDirection) -> Unit,
) {
    fun setUpSortButton(
        sortButton: Button,
        dataType: DataType,
        sortAndDirection: SortAndDirection,
    ) {
        val context = sortButton.context

        val listPopUp =
            ListPopupWindow(
                context,
                null,
                android.R.attr.listPopupWindowStyle,
            )
        // Resolve the strings, then sort
        val sortOptions =
            dataType.sortOptions
                .filter { serverVersion.isAtLeast(it.requiresVersion) }
                .map {
                    Pair(
                        it.key,
                        context.getString(it.nameStringId),
                    )
                }.sortedBy { it.second }
        val resolvedNames = sortOptions.map { it.second }

        val index =
            if (sortAndDirection.isRandom) {
                sortOptions.map { it.first }.indexOf(SortOption.RANDOM.key)
            } else {
                sortOptions.map { it.first }.indexOf(sortAndDirection.sort.key)
            }

        val resolvedName = resolvedNames[index]
        setSortButtonText(sortButton, resolvedName, sortAndDirection)

        val adapter =
            SortByArrayAdapter(
                context,
                resolvedNames,
                index,
                sortAndDirection.direction,
            )
        listPopUp.setAdapter(adapter)
        listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
        listPopUp.anchorView = sortButton

        listPopUp.width = getMaxMeasuredWidth(context, adapter)
        listPopUp.isModal = true

        listPopUp.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
            val newSortBy = sortOptions[position].first
            listPopUp.dismiss()

            val currentDirection = sortAndDirection.direction
            val currentKey = sortAndDirection.sort.key
            val newDirection =
                if (newSortBy == currentKey) {
                    currentDirection.toggle()
                } else {
                    currentDirection
                }
            val resolvedNewSortBy =
                if (newSortBy.startsWith("random")) {
                    "random_" + getRandomSort()
                } else {
                    newSortBy
                }

            val newSortAndDirection = SortAndDirection.create(resolvedNewSortBy, newDirection)
            Log.v(TAG, "newSortAndDirection=$newSortAndDirection")
            newSortCallback(newSortAndDirection)
            setUpSortButton(sortButton, dataType, newSortAndDirection)
        }

        sortButton.setOnClickListener {
            val currentDirection = sortAndDirection.direction
            val currentKey = sortAndDirection.sort.key
            val currentIndex = sortOptions.map { it.first }.indexOf(currentKey)
            adapter.currentIndex = currentIndex
            adapter.currentDirection = currentDirection

            listPopUp.show()
            listPopUp.listView?.requestFocus()
        }
    }

    private fun setSortButtonText(
        sortButton: Button,
        sortByStr: String,
        sortAndDirection: SortAndDirection,
    ) {
        val context = sortButton.context
        val directionString =
            when (sortAndDirection.direction) {
                SortDirectionEnum.ASC -> context.getString(R.string.fa_caret_up)
                SortDirectionEnum.DESC -> context.getString(R.string.fa_caret_down)
                SortDirectionEnum.UNKNOWN__ -> null
            }
        if (sortAndDirection.isRandom) {
            sortButton.text = context.getString(R.string.stashapp_random)
        } else if (directionString != null) {
            SpannableString("$directionString $sortByStr").apply {
                val start = 0
                val end = 1
                setSpan(
                    FontSpan(StashApplication.getFont(R.font.fa_solid_900)),
                    start,
                    end,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE,
                )
                sortButton.text = this
            }
        } else {
            sortButton.text = sortByStr
        }
    }

    fun SortDirectionEnum.toggle(): SortDirectionEnum = if (this == SortDirectionEnum.ASC) SortDirectionEnum.DESC else SortDirectionEnum.ASC

    private class SortByArrayAdapter(
        context: Context,
        items: List<String>,
        var currentIndex: Int,
        var currentDirection: SortDirectionEnum?,
    ) :
        ArrayAdapter<String>(context, R.layout.sort_popup_item, R.id.popup_item_text, items) {
        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup,
        ): View {
            val view = super.getView(position, convertView, parent)
            view as LinearLayout
            (view.get(0) as TextView).text =
                if (position == currentIndex) {
                    when (currentDirection) {
                        SortDirectionEnum.ASC -> context.getString(R.string.fa_caret_up)
                        SortDirectionEnum.DESC -> context.getString(R.string.fa_caret_down)
                        SortDirectionEnum.UNKNOWN__ -> null
                        null -> null
                    }
                } else {
                    null
                }
            return view
        }
    }

    companion object {
        private val TAG = SortButtonManager::class.java.simpleName
    }
}
