package com.github.damontecres.stashapp.filter.picker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.leanback.widget.picker.Picker
import androidx.leanback.widget.picker.PickerColumn
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.filter.CreateFilterViewModel
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

class IntPickerFragment(
    val title: String,
    val filterOption: FilterOption<SceneFilterType, IntCriterionInput>,
) : Fragment(R.layout.picker_number) {
    private val viewModel by activityViewModels<CreateFilterViewModel>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val filter = viewModel.filter.value!!
        val curVal = filterOption.getter.invoke(filter).getOrNull()

        val titleView = view.findViewById<TextView>(R.id.title)
        titleView.text = title

        val picker = view.findViewById<Picker>(R.id.number_picker)

        val modifierColumn = PickerColumn()
        modifierColumn.staticLabels = modifiers.map { it.getString(requireContext()) }.toTypedArray()
        modifierColumn.minValue = 0
        modifierColumn.maxValue = modifiers.size - 1

        val valueColumn = PickerColumn()
        valueColumn.labelFormat = "%1\$d"
        valueColumn.minValue = 0
        valueColumn.maxValue = 100

        picker.isActivated = true
        picker.setActivatedVisibleItemCount(5f)
        picker.separator = ""
        picker.setColumns(listOf(modifierColumn, valueColumn))
        picker.setColumnValue(0, modifiers.indexOf(curVal?.modifier).coerceAtLeast(0), true)
        picker.setColumnValue(1, curVal?.value ?: 0, true)

        picker.setOnClickListener {
            val modifier = modifiers[modifierColumn.currentValue]
            val value = valueColumn.currentValue
            viewModel.filter.value =
                filterOption.setter.invoke(
                    filter,
                    IntCriterionInput(
                        value = value,
                        modifier = modifier,
                    ),
                )
            parentFragmentManager.popBackStackImmediate()
        }
    }

    companion object {
        private val modifiers =
            listOf(
                CriterionModifier.EQUALS,
                CriterionModifier.NOT_EQUALS,
                CriterionModifier.GREATER_THAN,
                CriterionModifier.LESS_THAN,
            )
    }

    @SuppressLint("DefaultLocale")
    private fun staticLabels(): Array<CharSequence>? {
        return if (filterOption.type != IntCriterionInput::class) {
            buildList<CharSequence> {
                for (i in 0..<10_000) {
                    add(String.format("%.2f", i / 10.0))
                }
            }.toTypedArray()
        } else {
            null
        }
    }
}
