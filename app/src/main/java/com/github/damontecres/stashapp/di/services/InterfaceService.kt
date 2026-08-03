package com.github.damontecres.stashapp.di.services

import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class InterfaceService(
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(InterfaceServiceState())
    val state: StateFlow<InterfaceServiceState> = _state

    fun setTitle(title: String?) = title?.let { setTitle(AnnotatedString(it)) }

    fun setTitle(title: AnnotatedString?) {
        _state.update { it.copy(title = title) }
    }

    fun setTitleForPerformer(title: AnnotatedString) {
        val newStyles =
            if (title.spanStyles.size == 2) {
                listOf(
                    title.spanStyles[0].let {
                        it.copy(item = it.item)
                    },
                    title.spanStyles[1].let {
                        it.copy(item = it.item.copy(fontSize = title.spanStyles[0].item.fontSize * .75f))
                    },
                )
            } else if (title.spanStyles.size == 1) {
                listOf(
                    title.spanStyles[0].let {
                        it.copy(item = it.item)
                    },
                )
            } else {
                listOf()
            }

        val newTitle = AnnotatedString(title.text, newStyles, title.paragraphStyles)
        setTitle(newTitle)
    }
}

data class InterfaceServiceState(
    val title: AnnotatedString? = null,
)
