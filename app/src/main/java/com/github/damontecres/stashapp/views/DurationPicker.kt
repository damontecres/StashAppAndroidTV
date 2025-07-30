package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import androidx.leanback.widget.picker.Picker
import androidx.leanback.widget.picker.PickerColumn
import com.github.damontecres.stashapp.R

/**
 * [Picker] to select hours, minutes, & seconds as a duration
 */
class DurationPicker(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : Picker(context, attrs, defStyleAttr) {
    constructor(
        context: Context,
        attrs: AttributeSet?,
    ) : this(context, attrs, 0)

    constructor(context: Context) : this(context, null)

    var hours: Int
        get() = getColumnAt(0)!!.currentValue
        set(value) {
            setColumnValue(0, value, false)
        }
    var minutes: Int
        get() = getColumnAt(1)!!.currentValue
        set(value) {
            setColumnValue(1, value, false)
        }
    var seconds: Int
        get() = getColumnAt(2)!!.currentValue
        set(value) {
            setColumnValue(2, value, false)
        }

    var duration: Int
        get() = hours * 3600 + minutes * 60 + seconds
        set(value) {
            hours = value / 3600
            minutes = (value % 3600) / 60
            seconds = value % 60
        }

    init {
        val hourColumn =
            PickerColumn().apply {
                minValue = 0
                maxValue = 24
                labelFormat = " %d " + context.getString(R.string.hours)
            }
        val minuteColumn =
            PickerColumn().apply {
                minValue = 0
                maxValue = 59
                labelFormat = " %d " + context.getString(R.string.minutes)
            }
        val secondColumn =
            PickerColumn().apply {
                minValue = 0
                maxValue = 59
                labelFormat = " %d " + context.getString(R.string.stashapp_seconds)
            }
        separator = ":"
        setColumns(listOf(hourColumn, minuteColumn, secondColumn))
    }
}
