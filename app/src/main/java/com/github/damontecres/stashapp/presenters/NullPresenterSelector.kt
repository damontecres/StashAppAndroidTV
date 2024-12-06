package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector

/**
 * A [PresenterSelector] that delegates to another [PresenterSelector] except when the item is null, it return the specified [Presenter]
 */
class NullPresenterSelector(
    private val presenterSelector: PresenterSelector,
    private val nullPresenter: Presenter,
) : PresenterSelector() {
    override fun getPresenter(item: Any?): Presenter {
        return if (item == null) {
            nullPresenter
        } else {
            presenterSelector.getPresenter(item)
        }
    }
}
