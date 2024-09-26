package com.github.damontecres.stashapp.util

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import kotlin.reflect.KClass

fun <T : Parcelable> Intent.getParcelable(
    name: String,
    klass: KClass<T>,
): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, klass.java)
    } else {
        getParcelableExtra(name)
    }
}

fun <T : Parcelable> Bundle.getParcelable(
    name: String,
    klass: KClass<T>,
): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(name, klass.java)
    } else {
        getParcelable(name)
    }
}
