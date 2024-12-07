package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.MutableLiveData

/**
 * A [MutableLiveData] that only notifies observers if the value does not equal the previous value
 */
class EqualityMutableLiveData<T>(value: T?) : MutableLiveData<T>(value) {
    override fun setValue(value: T?) {
        if (value != getValue()) {
            super.setValue(value)
        }
    }

    override fun postValue(value: T?) {
        if (value != getValue()) {
            super.postValue(value)
        }
    }
}
