package com.github.damontecres.stashapp.ui.components.filter

import androidx.compose.ui.text.input.KeyboardType
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.ui.between
import com.github.damontecres.stashapp.ui.nullCheck
import java.util.Date
import kotlin.time.Duration.Companion.seconds

data class IdName(
    val id: String,
    val name: String,
)

data class InputTextAction(
    val title: String,
    val value: String?,
    val keyboardType: KeyboardType,
    val onSubmit: (String) -> Unit,
    val isValid: (String) -> Boolean = { true },
)

data class InputCriterionModifier(
    val filterName: String,
    val allowedModifiers: List<CriterionModifier>,
    val onClick: (CriterionModifier) -> Unit,
)

data class SelectFromListAction(
    val filterName: String,
    val options: List<String>,
    val currentOptions: List<String>,
    val multiSelect: Boolean,
    val onSubmit: (indices: List<Int>) -> Unit,
)

data class MultiCriterionInfo(
    val name: String,
    val dataType: DataType,
    val initialValues: List<IdName>,
    val onAdd: (IdName) -> Unit,
    val onSave: (List<IdName>) -> Unit,
)

data class InputDateAction(
    val name: String,
    val value: Date,
    val onSave: (Date) -> Unit,
)

data class InputDurationAction(
    val name: String,
    val value: Int?,
    val onSave: (Int) -> Unit,
)

interface SimpleCriterionInput<T : Comparable<T>> {
    val value: T
    val value2: T?
    val modifier: CriterionModifier

    val readable: String
        get() = value.toString()
    val readable2: String?
        get() = value2?.toString()

    fun isValid(): Boolean {
        if (modifier.between) {
            val val2 = value2
            return val2 != null && value < val2
        } else {
            return true
        }
    }
}

class SimpleIntCriterionInput(
    val input: IntCriterionInput,
) : SimpleCriterionInput<Int> {
    override val value: Int = input.value
    override val value2: Int? = input.value2.getOrNull()
    override val modifier: CriterionModifier = input.modifier
}

class SimpleFloatCriterionInput(
    val input: FloatCriterionInput,
) : SimpleCriterionInput<Double> {
    override val value: Double = input.value
    override val value2: Double? = input.value2.getOrNull()
    override val modifier: CriterionModifier = input.modifier
}

class SimpleStringCriterionInput(
    val input: StringCriterionInput,
) : SimpleCriterionInput<String> {
    override val value: String = input.value
    override val value2: String? = null
    override val modifier: CriterionModifier = input.modifier

    override fun isValid(): Boolean = modifier.nullCheck || value.isNotBlank()
}

class SimpleDateCriterionInput(
    val input: DateCriterionInput,
) : SimpleCriterionInput<String> {
    override val value: String = input.value
    override val value2: String? = input.value2.getOrNull()
    override val modifier: CriterionModifier = input.modifier
}

class SimpleDurationCriterionInput(
    val input: IntCriterionInput,
) : SimpleCriterionInput<Int> {
    override val value: Int = input.value
    override val value2: Int? = input.value2.getOrNull()
    override val modifier: CriterionModifier = input.modifier

    override val readable: String
        get() = value.seconds.toString()
    override val readable2: String?
        get() = value2?.seconds?.toString()
}

class SimpleRatingCriterionInput(
    val input: IntCriterionInput,
    val ratingAsStar: Boolean,
) : SimpleCriterionInput<Int> {
    private fun convert(v: Int) = if (ratingAsStar) v / 20.0 else v / 10.0

    override val value: Int = input.value
    override val value2: Int? = input.value2.getOrNull()

    override val modifier: CriterionModifier = input.modifier

    override val readable: String
        get() = convert(value).toString()

    override val readable2: String?
        get() = value2?.let { convert(it).toString() }

    override fun isValid(): Boolean {
        if (!modifier.nullCheck) {
            val range = if (ratingAsStar) 0.0..5.0 else 0.0..10.0
            if (readable.toDouble() !in range) {
                return false
            } else if (modifier.between &&
                (readable2 == null || readable2!!.toDouble() !in range || readable.toDouble() > readable2!!.toDouble())
            ) {
                return false
            }
        }
        return true
    }
}
