package com.github.damontecres.stashapp.views.models;

import androidx.lifecycle.MutableLiveData;

import java.util.Objects;

/**
 * A MutableLiveData that only notifies observers if the value does not equal the previous value
 * @param <T>
 */
public class EqualityMutableLiveData<T> extends MutableLiveData<T> {

    // Using Java since MutableLiveData is written in Java and it is easier to support the overrides
    public EqualityMutableLiveData(){
        super();
    }

    public EqualityMutableLiveData(T value){
        super(value);
    }

    @Override
    public void setValue(T value) {
        if(!Objects.equals(value, getValue())) {
            super.setValue(value);
        }
    }

    @Override
    public void postValue(T value) {
        if(!Objects.equals(value, getValue())) {
            super.postValue(value);
        }
    }
}
