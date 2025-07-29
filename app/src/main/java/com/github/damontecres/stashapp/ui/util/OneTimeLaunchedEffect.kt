package com.github.damontecres.stashapp.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope

/**
 * Run a [LaunchedEffect] exactly once even with multiple recompositions.
 *
 * If the composition is removed from the navigation back stack and "re-added", this will run again
 */
@Composable
fun OneTimeLaunchedEffect(runOnceBlock: suspend CoroutineScope.() -> Unit) {
    var hasRun by rememberSaveable { mutableStateOf(false) }
    if (!hasRun) {
        LaunchedEffect(Unit) {
            hasRun = true
            runOnceBlock.invoke(this)
        }
    }
}
