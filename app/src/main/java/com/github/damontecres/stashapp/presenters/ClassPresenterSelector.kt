package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector

class ClassPresenterSelector : PresenterSelector() {
    private val mPresenters = ArrayList<Presenter>()

    private val mClassMap = HashMap<Class<*>?, Any>()

    /**
     * Sets a presenter to be used for the given class.
     * @param cls The data model class to be rendered.
     * @param presenter The presenter that renders the objects of the given class.
     * @return This ClassPresenterSelector object.
     */
    fun addClassPresenter(
        cls: Class<*>,
        presenter: Presenter,
    ): ClassPresenterSelector {
        mClassMap[cls] = presenter
        if (!mPresenters.contains(presenter)) {
            mPresenters.add(presenter)
        }
        return this
    }

    /**
     * Sets a presenter selector to be used for the given class.
     * @param cls The data model class to be rendered.
     * @param presenterSelector The presenter selector that finds the right presenter for a given
     * class.
     * @return This ClassPresenterSelector object.
     */
    fun addClassPresenterSelector(
        cls: Class<*>,
        presenterSelector: PresenterSelector,
    ): ClassPresenterSelector {
        mClassMap[cls] = presenterSelector
        val innerPresenters = presenterSelector.presenters
        for (i in innerPresenters.indices) {
            if (!mPresenters.contains(innerPresenters[i])) {
                mPresenters.add(innerPresenters[i])
            }
        }
        return this
    }

    override fun getPresenter(item: Any): Presenter {
        var cls: Class<*>? = item.javaClass
        var presenter: Any? = null

        do {
            presenter = mClassMap[cls]
            if (presenter is PresenterSelector) {
                val innerPresenter = presenter.getPresenter(item)
                if (innerPresenter != null) {
                    return innerPresenter
                }
            }
            cls = cls!!.superclass
        } while (presenter == null && cls != null)

        return presenter as Presenter
    }

    fun getPresenter(javaClass: Class<*>): Presenter {
        var cls: Class<*>? = javaClass
        var presenter: Any?
        do {
            presenter = mClassMap[cls]
            cls = cls!!.superclass
        } while (presenter == null && cls != null)
        return presenter as Presenter
    }

    override fun getPresenters(): Array<Presenter> = mPresenters.toTypedArray<Presenter>()
}
