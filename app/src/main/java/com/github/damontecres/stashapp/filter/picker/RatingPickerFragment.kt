package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.text.InputType
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.FilterOption
import kotlin.properties.Delegates

class RatingPickerFragment(
    filterOption: FilterOption<StashDataFilter, IntCriterionInput>,
) : TwoValuePicker<Int, IntCriterionInput>(filterOption) {
    private var ratingsAsStars by Delegates.notNull<Boolean>()

    override val modifierOptions: List<CriterionModifier>
        get() =
            super.modifierOptions +
                listOf(
                    CriterionModifier.IS_NULL,
                    CriterionModifier.NOT_NULL,
                )

    override val valueInputType: Int
        get() = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        ratingsAsStars =
            viewModel.server.value!!
                .serverPreferences.ratingsAsStars
        super.onCreate(savedInstanceState)
    }

    override fun parseValue(v: String?): Int? {
        val value = v?.toDoubleOrNull() ?: return null
        val rating100 =
            if (ratingsAsStars) {
                (value.times(20)).toInt()
            } else {
                (value.times(10)).toInt()
            }
        return if (rating100 in 0..100) {
            rating100
        } else {
            null
        }
    }

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

    override fun formatDescription(v: Int?): String? =
        if (ratingsAsStars) {
            v?.div(20.0)
        } else {
            v?.div(10.0)
        }?.toString()

    companion object {
        private const val TAG = "RatingPickerFragment"
    }
}
