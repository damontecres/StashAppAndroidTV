package com.github.damontecres.stashapp.util

import android.service.dreams.DreamService
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.imageLoader
import coil3.request.ImageRequest
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.components.screensaver.ScreensaverContent
import com.github.damontecres.stashapp.ui.components.screensaver.ScreensaverFilter
import com.github.damontecres.stashapp.ui.components.screensaver.getScreensaverFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.android.ext.android.get
import kotlin.time.Duration.Companion.seconds

class StashDreamService :
    DreamService(),
    SavedStateRegistryOwner {
    private val serverRepository: ServerRepository = get()
    private val queryEngine: QueryEngine = get()

    private val lifecycleRegistry = LifecycleRegistry(this)

    private val savedStateRegistryController =
        SavedStateRegistryController.create(this).apply {
            performAttach()
        }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val itemFlow = createFlow()
        setContentView(
            ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@StashDreamService)
                setViewTreeSavedStateRegistryOwner(this@StashDreamService)
                setContent {
                    AppTheme {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                        ) {
                            val currentItem by itemFlow.collectAsState(null)
                            ScreensaverContent(
                                imageData = currentItem,
                                duration = 60.seconds,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            },
        )
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun createFlow(): Flow<ImageData> =
        flow {
            serverRepository.restore()
            val current = serverRepository.currentServer.value
            val file = getScreensaverFile(this@StashDreamService, current.server)
            val screensaverFilter =
                try {
                    if (file.exists()) {
                        Log.d(TAG, "Reading ScreensaverFilter")
                        ScreensaverFilter.read(file)
                    } else {
                        ScreensaverFilter.makeDefault()
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Error loading file", ex)
                    ScreensaverFilter.makeDefault()
                }
            val filter =
                if (screensaverFilter.savedFilterId.isNotNullOrBlank()) {
                    Log.v(TAG, "Fetching saved filter ${screensaverFilter.savedFilterId}")
                    queryEngine
                        .getSavedFilter(screensaverFilter.savedFilterId)
                        ?.toFilterArgs(FilterParser(current.serverPreferences.version))
                } else {
                    screensaverFilter.filter
                } ?: screensaverFilter.filter
            val dataSupplier =
                DataSupplierFactory(current.serverPreferences.version).create<Query.Data, StashData, Query.Data>(
                    filter,
                )
            val pagingSource =
                StashPagingSource<Query.Data, StashData, StashData, Query.Data>(
                    queryEngine,
                    dataSupplier = dataSupplier,
                )
            val pager =
                ComposePager(
                    filter = filter,
                    source = pagingSource,
                    scope = lifecycleScope,
                    pageSize = 25,
                    cacheSize = 2,
                )
            pager.init()
            Log.v(TAG, "Got ${pager.size} images")
            var index = 0
            while (true) {
                val imageData =
                    try {
                        pager.getBlocking(index) as ImageData?
                    } catch (ex: Exception) {
                        Log.w(TAG, "Error fetching image", ex)
                        null
                    }
                if (imageData != null) {
                    this@StashDreamService
                        .imageLoader
                        .enqueue(
                            ImageRequest
                                .Builder(this@StashDreamService)
                                .data(imageData.paths.image)
                                .build(),
                        ).job
                        .await()
                    emit(imageData)
                    delay(60.seconds)
                    index++
                    if (index >= pager.size) index = 0
                }
            }
        }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "StashDreamService"
    }
}
