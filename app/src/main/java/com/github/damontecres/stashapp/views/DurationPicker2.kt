package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import androidx.leanback.widget.picker.Picker
import androidx.leanback.widget.picker.PickerColumn
import com.github.damontecres.stashapp.R

/**
 * [Picker] to select hours, minutes, seconds, & milliseconds timestamp
 */
class DurationPicker2(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : Picker(context, attrs, defStyleAttr) {
    constructor(
        context: Context,
        attrs: AttributeSet?,
    ) : this(context, attrs, 0)

    constructor(context: Context) : this(context, null)

    private var hoursEnabled = true
    private var minutesEnabled = true
    private var secondsEnabled = true

    private var hours: Int
        get() = getColumnAt(0).currentValue
        set(value) {
            setColumnValue(0, value, false)
        }
    private var minutes: Int
        get() = getColumnAt(columnsCount - 3).currentValue
        set(value) {
            setColumnValue(columnsCount - 3, value, false)
        }
    private var seconds: Int
        get() = getColumnAt(columnsCount - 2).currentValue
        set(value) {
            setColumnValue(columnsCount - 2, value, false)
        }
    private var milliseconds: Int
        get() = getColumnAt(columnsCount - 1).currentValue
        set(value) {
            setColumnValue(columnsCount - 1, value, false)
        }

    var duration: Long
        get() {
            var result = milliseconds.toLong() * MILLISECONDS_STEP
            if (minutesEnabled) result += minutes * 60 * 1000
            if (hoursEnabled) result += hours * 3600 * 1000
            if (secondsEnabled) result += seconds * 1000
            return result
        }
        set(value) {
            if (hoursEnabled) {
                hours = getHours(value)
            }
            if (minutesEnabled) {
                minutes = getMinutes(value)
            }
            if (secondsEnabled) {
                seconds = getSeconds(value)
            }
            milliseconds = getMilliseconds(value) / MILLISECONDS_STEP
        }

    init {
        separator = ":"
    }

    fun setMaxDuration(value: Long) {
        hoursEnabled = getHours(value) >= 1
        minutesEnabled = getMinutes(value) >= 1
        secondsEnabled = getSeconds(value) >= 1

        val hourColumn =
            PickerColumn().apply {
                minValue = 0
                maxValue = getHours(value)
                labelFormat = " %d " + context.getString(R.string.hours)
            }
        val minuteColumn =
            PickerColumn().apply {
                minValue = 0
                maxValue = if (hoursEnabled) 59 else getMinutes(value)
                labelFormat = " %d " + context.getString(R.string.minutes)
            }
        val secondColumn =
            PickerColumn().apply {
                minValue = 0
                maxValue = if (minutesEnabled) 59 else getSeconds(value)
                labelFormat = " %d " + context.getString(R.string.stashapp_seconds)
            }
        val millisecondColumn =
            PickerColumn().apply {
                minValue = 0
                maxValue = 1000 / MILLISECONDS_STEP - 1
                staticLabels =
                    (0..<(1000 / MILLISECONDS_STEP))
                        .map { " ${it * MILLISECONDS_STEP} " + context.getString(R.string.milliseconds) }
                        .toTypedArray()
//                labelFormat = " %d " + context.getString(R.string.milliseconds)
            }
        if (hoursEnabled) {
            setColumns(listOf(hourColumn, minuteColumn, secondColumn, millisecondColumn))
        } else if (minutesEnabled) {
            setColumns(listOf(minuteColumn, secondColumn, millisecondColumn))
        } else if (secondsEnabled) {
            setColumns(listOf(secondColumn, millisecondColumn))
        } else {
            setColumns(listOf(millisecondColumn))
        }
    }

    companion object {
        private const val TAG = "DurationPicker"
        private const val MILLISECONDS_STEP = 50

        private fun getHours(value: Long) = (value / (3600 * 1000)).toInt()

        private fun getMinutes(value: Long) = ((value % (3600 * 1000)) / (60 * 1000)).toInt()

        private fun getSeconds(value: Long) = (value % (60 * 1000) / 1000).toInt()

        private fun getMilliseconds(value: Long) = (value % 1000).toInt()
    }
}
