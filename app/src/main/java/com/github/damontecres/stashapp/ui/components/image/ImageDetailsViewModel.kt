package com.github.damontecres.stashapp.ui.components.image

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.VideoFilter
import com.github.damontecres.stashapp.data.room.AppDatabase
import com.github.damontecres.stashapp.data.room.PlaybackEffect
import com.github.damontecres.stashapp.di.server.MutationEngine
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.PlayerFactory
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.ui.galleryId
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.showSetRatingToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import timber.log.Timber
import kotlin.properties.Delegates

@KoinViewModel
class ImageDetailsViewModel(
    private val serverRepository: ServerRepository,
    private val serverLogger: ServerLogger,
    private val queryEngine: QueryEngine,
    private val mutationEngine: MutationEngine,
    private val database: AppDatabase,
    val navigationManager: NavigationManager,
    val playerFactory: PlayerFactory,
    @InjectedParam private val filterArgs: FilterArgs,
    @InjectedParam private val startPosition: Int,
) : ViewModel() {
    private var saveFilters = true
    private lateinit var exceptionHandler: LoggingCoroutineExceptionHandler

    private val _state = MutableStateFlow(ImageDetailsPageState())
    val state: StateFlow<ImageDetailsPageState> = _state

    private val _imageFilter = MutableStateFlow(VideoFilter())
    val imageFilter: StateFlow<VideoFilter> = _imageFilter

    var slideshowDelay by Delegates.notNull<Long>()

    private var galleryImageFilter = VideoFilter()

    fun init(
        slideshow: Boolean,
        slideshowDelay: Long,
        saveFilters: Boolean,
    ): ImageDetailsViewModel {
        Log.v(TAG, "View model init")
        this.saveFilters = saveFilters
        this.slideshowDelay = slideshowDelay
        val pager = state.value.pager
        if (pager !is ComposePager<*> || pager.filter != filterArgs) {
            if (filterArgs.dataType != DataType.IMAGE) {
                throw IllegalArgumentException("Cannot use ${filterArgs.dataType}")
            }
            _state.update {
                it.copy(galleryId = (filterArgs.objectFilter as? ImageFilterType)?.galleryId)
            }
            this.exceptionHandler =
                LoggingCoroutineExceptionHandler(
                    serverRepository.currentServer.value,
                    viewModelScope,
                    toastMessage = "Error updating image",
                )
            val dataSupplierFactory = DataSupplierFactory(serverRepository.currentServerVersion)
            val dataSupplier =
                dataSupplierFactory.create<Query.Data, ImageData, Query.Data>(filterArgs)
            val pagingSource =
                StashPagingSource(
                    queryEngine,
                    dataSupplier,
                ) { _, _, item -> item }
            val pager = ComposePager(filterArgs, pagingSource, viewModelScope)
            Log.v(TAG, "Pager created: filterArgs=$filterArgs")
            viewModelScope.launch(
                LoggingCoroutineExceptionHandler(
                    serverRepository.currentServer.value,
                    viewModelScope,
                ),
            ) {
                pager.init()
                Log.v(TAG, "Pager size: ${pager.size}")
                _state.update {
                    it.copy(
                        pager = pager,
                        slideshow = slideshow,
                    )
                }
                updatePosition(startPosition)
                if (slideshow) {
                    startSlideshow()
                    pulseSlideshow()
                }
            }
            state.value.galleryId?.let { galleryId ->
                viewModelScope.launchIO {
                    viewModelScope.launchIO(StashCoroutineExceptionHandler()) {
                        val server = serverRepository.currentServer.value.server
                        val vf =
                            database
                                .playbackEffectsDao()
                                .getPlaybackEffect(server.url, galleryId, DataType.GALLERY)
                        if (vf != null && vf.videoFilter.hasImageFilter()) {
                            Log.d(
                                TAG,
                                "Loaded VideoFilter for gallery $galleryId",
                            )
                            galleryImageFilter = vf.videoFilter
                            updateImageFilter(vf.videoFilter)
                        }
                    }
                }
            }
        }

        return this
    }

    fun nextImage(): Boolean {
        val size = state.value.pager.size
        val newPosition = state.value.position + 1
        return if (newPosition < size) {
            updatePosition(newPosition)
            true
        } else {
            false
        }
    }

    fun previousImage(): Boolean {
        val newPosition = state.value.position - 1
        return if (newPosition >= 0) {
            updatePosition(newPosition)
            true
        } else {
            false
        }
    }

    fun updatePosition(position: Int) {
        state.value.pager.let { pager ->
            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                try {
                    if (pager.isEmpty() || position !in pager.indices) {
                        return@launch
                    }
                    pager as ComposePager<ImageData>
                    val image = pager.getBlocking(position)
                    Log.v(TAG, "Got image for $position: ${image != null}")
                    if (image != null) {
                        _state.update {
                            it.copy(
                                position = position,
                                rating100 = image.rating100 ?: 0,
                                oCount = image.o_counter ?: 0,
                                tags = emptyList(),
                                performers = emptyList(),
                                galleries = emptyList(),
                            )
                        }
                        // reset image filter
                        updateImageFilter(galleryImageFilter)
                        if (saveFilters) {
                            viewModelScope.launchIO(StashCoroutineExceptionHandler()) {
                                val server = serverRepository.currentServer.value.server
                                val vf =
                                    database
                                        .playbackEffectsDao()
                                        .getPlaybackEffect(server!!.url, image.id, DataType.IMAGE)
                                if (vf != null && vf.videoFilter.hasImageFilter()) {
                                    Log.d(
                                        TAG,
                                        "Loaded VideoFilter for image ${image.id}",
                                    )
                                    updateImageFilter(vf.videoFilter)
                                }
                            }
                        }
                        _state.update {
                            it.copy(
                                image = image,
                                loadingState = ImageLoadingState.Success(image),
                            )
                        }
                        if (image.tags.isNotEmpty()) {
                            val tags =
                                queryEngine.getTags(image.tags.map { it.id })
                            Log.v(TAG, "Got ${tags.size} tags")
                            _state.update { it.copy(tags = tags) }
                        }
                        if (image.performers.isNotEmpty()) {
                            val performers =
                                queryEngine.findPerformers(performerIds = image.performers.map { it.id })
                            _state.update { it.copy(performers = performers) }
                        }
                        if (image.galleries.isNotEmpty()) {
                            val galleries =
                                queryEngine.findGalleries(galleryIds = image.galleries.map { it.id })
                            _state.update { it.copy(galleries = galleries) }
                        }
                    } else {
                        _state.update {
                            it.copy(
                                image = null,
                                loadingState = ImageLoadingState.Error,
                            )
                        }
                    }
                    if (position + 1 in pager.indices) {
                        try {
                            pager.getBlocking(position + 1)?.let { nextImage ->
                                Timber.v("Prefetching %s", nextImage.id)
                                val request =
                                    ImageRequest
                                        .Builder(StashApplication.getApplication())
                                        .data(nextImage.paths.image)
                                        .size(Size.ORIGINAL)
                                        .build()
                                StashApplication.getApplication().imageLoader.enqueue(request)
                            }
                        } catch (ex: Exception) {
                            Timber.e(ex, "Error prefetching image")
                        }
                    }
                } catch (ex: Exception) {
                    _state.update { it.copy(loadingState = ImageLoadingState.Error) }
                    LoggingCoroutineExceptionHandler(
                        serverRepository.currentServer.value,
                        viewModelScope,
                        toastMessage = "Error fetching image",
                    ).handleException(ex)
                }
            }
        }
    }

    fun addTag(
        imageId: String,
        tagId: String,
    ) = mutateTags(imageId) { add(tagId) }

    fun removeTag(
        imageId: String,
        tagId: String,
    ) = mutateTags(imageId) { remove(tagId) }

    private fun mutateTags(
        imageId: String,
        mutator: MutableList<String>.() -> Unit,
    ) {
        val ids = state.value.tags.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(exceptionHandler) {
                val result = mutationEngine.updateImage(imageId = imageId, tagIds = mutable)
                if (result != null) {
                    _state.update { it.copy(tags = result.tags.map { it.tagData }) }
                }
            }
        }
    }

    fun addPerformer(
        imageId: String,
        performerId: String,
    ) = mutatePerformers(imageId) { add(performerId) }

    fun removePerformer(
        imageId: String,
        performerId: String,
    ) = mutatePerformers(imageId) { remove(performerId) }

    private fun mutatePerformers(
        imageId: String,
        mutator: MutableList<String>.() -> Unit,
    ) {
        val perfs = state.value.performers.map { it.id }
        perfs?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(exceptionHandler) {
                val result = mutationEngine.updateImage(imageId = imageId, performerIds = mutable)
                if (result != null) {
                    _state.update { it.copy(performers = result.performers.map { it.performerData }) }
                }
            }
        }
    }

    fun updateRating(
        imageId: String,
        rating100: Int,
    ) {
        viewModelScope.launch(exceptionHandler) {
            val newRating =
                mutationEngine.updateImage(imageId, rating100 = rating100)?.rating100 ?: 0
            _state.update { it.copy(rating100 = newRating) }
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }

    fun updateOCount(action: suspend MutationEngine.(String) -> OCounter) {
        viewModelScope.launch(exceptionHandler) {
            state.value.image?.let {
                val newOCount = action.invoke(mutationEngine, it.id)
                _state.update { it.copy(oCount = newOCount.count) }
            }
        }
    }

    private var slideshowJob: Job? = null

    fun startSlideshow() {
        _state.update { it.copy(slideshow = true, slideshowPaused = false) }
        if (state.value.image?.isImageClip == false) {
            pulseSlideshow()
        }
    }

    fun stopSlideshow() {
        slideshowJob?.cancel()
        _state.update { it.copy(slideshow = false) }
    }

    fun pauseSlideshow() {
        if (state.value.slideshow) {
            Log.v(TAG, "pauseSlideshow")
            _state.update { it.copy(slideshowPaused = true) }
            slideshowJob?.cancel()
        }
    }

    fun unpauseSlideshow() {
        if (state.value.slideshow) {
            Log.v(TAG, "unpauseSlideshow")
            _state.update { it.copy(slideshowPaused = false) }
        }
    }

    fun pulseSlideshow() = pulseSlideshow(slideshowDelay)

    fun pulseSlideshow(milliseconds: Long) {
        Log.v(TAG, "pulseSlideshow $milliseconds")
        slideshowJob?.cancel()
        if (state.value.slideshow) {
            slideshowJob =
                viewModelScope
                    .launch(StashCoroutineExceptionHandler()) {
                        delay(milliseconds)
                        Log.v(TAG, "pulseSlideshow after delay")
                        if (state.value.slideshowActive) {
                            nextImage()
                        }
                    }.apply {
                        invokeOnCompletion { if (it !is CancellationException) pulseSlideshow() }
                    }
        }
    }

    fun updateImageFilter(newFilter: VideoFilter) {
        _imageFilter.update { newFilter }
    }

    fun saveImageFilter() {
        state.value.image?.let {
            viewModelScope.launchIO(StashCoroutineExceptionHandler(autoToast = true)) {
                val server = serverRepository.currentServer.value.server
                val vf = _imageFilter.value
                if (vf != null) {
                    database
                        .playbackEffectsDao()
                        .insert(PlaybackEffect(server!!.url, it.id, DataType.IMAGE, vf))
                    Log.d(TAG, "Saved VideoFilter for image ${it.id}")
                    withContext(Dispatchers.Main) {
                        Toast
                            .makeText(
                                StashApplication.getApplication(),
                                "Saved",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }
        }
    }

    fun saveGalleryFilter() {
        state.value.galleryId?.let { galleryId ->
            viewModelScope.launchIO(StashCoroutineExceptionHandler(autoToast = true)) {
                val server = serverRepository.currentServer.value.server
                val vf = imageFilter.value
                if (vf != null) {
                    galleryImageFilter = vf
                    database
                        .playbackEffectsDao()
                        .insert(PlaybackEffect(server!!.url, galleryId, DataType.GALLERY, vf))
                    Log.d(TAG, "Saved VideoFilter for gallery $galleryId")
                    withContext(Dispatchers.Main) {
                        Toast
                            .makeText(
                                StashApplication.getApplication(),
                                "Saved",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ImageDetailsViewModel"
    }
}

interface SlideshowControls {
    fun startSlideshow()

    fun stopSlideshow()
}

sealed class ImageLoadingState {
    data object Loading : ImageLoadingState()

    data object Error : ImageLoadingState()

    data class Success(
        val image: ImageData,
    ) : ImageLoadingState()
}

data class ImageDetailsPageState(
    val slideshow: Boolean = false,
    val slideshowPaused: Boolean = false,
    val position: Int = 0,
    val image: ImageData? = null,
    val pager: List<ImageData?> = emptyList(),
    val loadingState: ImageLoadingState = ImageLoadingState.Loading,
    val tags: List<TagData> = emptyList(),
    val performers: List<PerformerData> = emptyList(),
    val galleries: List<GalleryData> = emptyList(),
    val rating100: Int = 0,
    val oCount: Int = 0,
    val galleryId: String? = null,
) {
    val slideshowActive: Boolean = slideshow && !slideshowPaused
}
