package com.github.damontecres.stashapp.filter.picker

import android.content.Context
import android.os.Bundle
import androidx.leanback.widget.GuidedAction

/**
 * [GuidedAction] for picking a duration
 */
class GuidedDurationPickerAction : GuidedAction() {
    class Builder(
        context: Context,
    ) : BuilderBase<Builder>(context) {
        private var duration = 0

        init {
            hasEditableActivatorView(true)
        }

        fun duration(duration: Int): Builder {
            this.duration = duration
            return this
        }

        fun build(): GuidedDurationPickerAction {
            val action = GuidedDurationPickerAction()
            action.duration = duration
            super.applyValues(action)
            return action
        }
    }

    var duration: Int = 0

    override fun onSaveInstanceState(
        bundle: Bundle,
        key: String,
    ) {
        bundle.putInt(key, duration)
    }

    override fun onRestoreInstanceState(
        bundle: Bundle,
        key: String,
    ) {
        duration = bundle.getInt(key)
    }
}
