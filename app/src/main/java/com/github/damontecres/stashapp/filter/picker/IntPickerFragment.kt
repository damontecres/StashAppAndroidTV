package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.text.InputType
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.FilterOption

class IntPickerFragment(
    filterOption: FilterOption<StashDataFilter, IntCriterionInput>,
) : TwoValuePicker<Int, IntCriterionInput>(filterOption) {
    override val valueInputType: Int
        get() = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

    override fun parseValue(v: String?): Int? = v?.toIntOrNull()

    override fun createCriterionInput(
        value1: Int?,
        value2: Int?,
        modifier: CriterionModifier,
    ): IntCriterionInput? =
        if (value1 != null) {
            IntCriterionInput(
                value = value1,
                value2 = Optional.presentIfNotNull(value2),
                modifier = modifier,
            )
        } else if (modifier == CriterionModifier.IS_NULL || modifier == CriterionModifier.NOT_NULL) {
            IntCriterionInput(value = 0, modifier = modifier)
        } else {
            null
        }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        val curVal = filterOption.getter(viewModel.objectFilter.value!!).getOrNull()
        value1 = curVal?.value
        value2 = curVal?.value2?.getOrNull()
        modifier = curVal?.modifier ?: CriterionModifier.EQUALS
        createActionList(actions)
    }

    companion object {
        private const val TAG = "IntPickerFragment"
    }
}
