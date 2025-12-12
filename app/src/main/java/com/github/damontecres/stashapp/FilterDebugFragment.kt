package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.util.toReadableString
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.github.damontecres.stashapp.views.models.StashGridViewModel

class FilterDebugFragment : Fragment(R.layout.filter_debug) {
    private val serverViewModel: ServerViewModel by activityViewModels()
    private val viewModel: StashGridViewModel by viewModels(ownerProducer = { requireParentFragment() })

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val filterNameView = view.findViewById<TextView>(R.id.filter_name)
        val filterDataTypeView = view.findViewById<TextView>(R.id.filter_datatype)
        val filterFindFilterView = view.findViewById<TextView>(R.id.filter_findfilter)
        val filterObjectFilterView = view.findViewById<TextView>(R.id.filter_objectfilter)

        val itemListView = view.findViewById<ViewGroup>(R.id.item_list)
        val itemTextView = view.findViewById<TextView>(R.id.items_text_view)

        val filter = viewModel.filterArgs.value
        filterNameView.text = filter?.name.toString()
        filterDataTypeView.text = filter?.dataType?.name.toString()
        filterFindFilterView.text = filter?.findFilter.toString()
        filterObjectFilterView.text = filter?.objectFilter?.toReadableString(true).toString()

        val ids = mutableSetOf<String>()
        when (val status = viewModel.loadingStatus.value) {
            is StashGridViewModel.LoadingStatus.AdapterReady -> {
                val pages = status.pagingAdapter.getPages()
                itemTextView.text = "Items (${status.pagingAdapter.size()})"
                pages.values.sortedBy { it.number }.forEach { page ->
                    val pageLayout = layoutInflater.inflate(R.layout.filter_debug_page, itemListView, false)
                    val table = pageLayout.findViewById<TableLayout>(R.id.page_table)
                    itemListView.addView(pageLayout)
                    val pageNumView = pageLayout.findViewById<TextView>(R.id.page_number_text)
                    pageNumView.text = "Page #${page.number}"
                    page.items.forEachIndexed { index, item ->
                        item as StashData
                        val rowView = layoutInflater.inflate(R.layout.filter_debug_page_row, table, false) as TableRow
                        val positionView = rowView[0] as TextView
                        val idView = rowView[1] as TextView
                        val titleView = rowView[2] as TextView
                        positionView.text = ((page.number - 1) * status.pagingAdapter.pageSize + index + 1).toString()
                        idView.text = item.id
                        titleView.text = extractTitle(item)

                        if (!ids.add(item.id)) {
                            rowView.setBackgroundColor(resources.getColor(R.color.transparent_red_50, null))
                        }
                        table.addView(rowView)
                    }
//                    table.isStretchAllColumns = true
                }
            }

            else -> {
                val titleView = TextView(requireContext())
                titleView.text = "No items"
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                itemListView.addView(titleView)
            }
        }
        view.requestFocus()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireParentFragment().childFragmentManager.commit {
                remove(this@FilterDebugFragment)
            }
        }
    }
}
