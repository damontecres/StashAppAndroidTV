package com.github.damontecres.stashapp.di.services

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.ui.text.AnnotatedString
import com.github.damontecres.stashapp.di.DefaultCoroutineScope
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.launchDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single
import timber.log.Timber
import java.util.WeakHashMap

@Single
class InterfaceService(
    private val application: Application,
    private val navigationManager: NavigationManager,
    @param:DefaultCoroutineScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(InterfaceServiceState())
    val state: StateFlow<InterfaceServiceState> = _state

    private val titleMap = WeakHashMap<Destination, AnnotatedString?>()

    init {
        scope.launchDefault {
            navigationManager.destinations.collectLatest { destinations ->
                if (destinations.isNotEmpty()) {
                    val current = destinations.last()
                    val title = titleMap[current] ?: AnnotatedString("")
                    setTitle(title)
                } else {
                    setTitle("")
                }
            }
        }
    }

    fun setTitle(title: String?) = setTitle(AnnotatedString(title ?: ""))

    fun setTitle(title: AnnotatedString?) {
        navigationManager.current?.let { current ->
            Timber.v("Setting title for %s", current)
            titleMap[current] = title
            _state.update { it.copy(title = title) }
        }
    }

    fun setTitle(
        title: String?,
        @StringRes fallback: Int,
    ) {
        if (title.isNotNullOrBlank()) {
            setTitle(AnnotatedString(title))
        } else {
            setTitle(AnnotatedString(application.getString(fallback)))
        }
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
