package com.github.damontecres.stashapp.ui.components.image

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.ThrottledLiveData
import com.github.damontecres.stashapp.data.VideoFilter
import com.github.damontecres.stashapp.data.room.PlaybackEffect
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.showSetRatingToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class ImageDetailsViewModel : ViewModel() {
    private var server: StashServer? = null
    private var saveFilters = true

    private val _slideshow = MutableLiveData(false)

    /**
     * Whether slideshow mode is on or off
     */
    val slideshow: LiveData<Boolean> = _slideshow
    private val _slideshowPaused = MutableLiveData(false)
    val slideshowPaused: LiveData<Boolean> = _slideshowPaused

    /**
     * Whether the slideshow is actively running meaning slideshow mode is ON and is currently NOT paused
     */
    val slideshowActive =
        slideshow
            .asFlow()
            .combine(slideshowPaused.asFlow()) { slideshow, paused ->
                slideshow && !paused
            }.asLiveData()

    var slideshowDelay by Delegates.notNull<Long>()

    val pager = MutableLiveData<ComposePager<ImageData>>()
    private var position = 0

    private val _image = MutableLiveData<ImageData>()
    val image: LiveData<ImageData> = _image

    val loadingState = MutableLiveData<ImageLoadingState>(ImageLoadingState.Loading)
    val tags = MutableLiveData<List<TagData>>(listOf())
    val performers = MutableLiveData<List<PerformerData>>(listOf())

    val rating100 = MutableLiveData(0)
    val oCount = MutableLiveData(0)
    private val _imageFilter = MutableLiveData(VideoFilter())
    val imageFilter = ThrottledLiveData(_imageFilter, 500L)

    fun init(
        server: StashServer,
        filterArgs: FilterArgs,
        startPosition: Int,
        slideshow: Boolean,
        slideshowDelay: Long,
        saveFilters: Boolean,
    ): ImageDetailsViewModel {
        Log.v(TAG, "View model init")
        this.saveFilters = saveFilters
        this.slideshowDelay = slideshowDelay
        if (pager.value?.filter != filterArgs || server != this.server) {
            if (filterArgs.dataType != DataType.IMAGE) {
                throw IllegalArgumentException("Cannot use ${filterArgs.dataType}")
            }
            this.server = server
            val dataSupplierFactory = DataSupplierFactory(server.version)
            val dataSupplier =
                dataSupplierFactory.create<Query.Data, ImageData, Query.Data>(filterArgs)
            val pagingSource =
                StashPagingSource(QueryEngine(server), dataSupplier) { _, _, item -> item }
            val pager = ComposePager(filterArgs, pagingSource, viewModelScope)
            Log.v(TAG, "Pager created: filterArgs=$filterArgs")
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                pager.init()
                Log.v(TAG, "Pager size: ${pager.size}")
                this@ImageDetailsViewModel.pager.value = pager
                this@ImageDetailsViewModel._slideshow.value = slideshow
                updatePosition(startPosition)
                if (slideshow) {
                    startSlideshow()
                    pulseSlideshow()
                }
            }
        }

        return this
    }

    fun nextImage(): Boolean {
        val size = pager.value?.size
        val newPosition = position + 1
        return if (size != null && newPosition < size) {
            updatePosition(newPosition)
            true
        } else {
            false
        }
    }

    fun previousImage(): Boolean {
        val newPosition = position - 1
        return if (newPosition >= 0) {
            updatePosition(newPosition)
            true
        } else {
            false
        }
    }

    fun updatePosition(position: Int) {
        pager.value?.let { pager ->
            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                try {
                    val image = pager.getBlocking(position)
                    Log.v(TAG, "Got image for $position: ${image != null}")
                    if (image != null) {
                        this@ImageDetailsViewModel.position = position
                        val queryEngine = QueryEngine(server!!)
                        rating100.value = image.rating100 ?: 0
                        oCount.value = image.o_counter ?: 0
                        tags.value = listOf()
                        performers.value = listOf()
                        // reset image filter
                        updateImageFilter(VideoFilter())
                        if (saveFilters) {
                            viewModelScope.launchIO(StashCoroutineExceptionHandler()) {
                                val vf =
                                    StashApplication
                                        .getDatabase()
                                        .playbackEffectsDao()
                                        .getPlaybackEffect(server!!.url, image.id, DataType.IMAGE)
                                if (vf != null) {
                                    Log.d(
                                        TAG,
                                        "Loaded VideoFilter for image ${image.id}",
                                    )
                                    withContext(Dispatchers.Main) {
                                        // Pause throttling so that the image loads with the filter applied immediately
                                        imageFilter.stopThrottling(true)
                                        updateImageFilter(vf.videoFilter)
                                        imageFilter.startThrottling()
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    _image.value = image
                                    loadingState.value = ImageLoadingState.Success(image)
                                }
                            }
                        } else {
                            _image.value = image
                            loadingState.value = ImageLoadingState.Success(image)
                        }
                        if (image.tags.isNotEmpty()) {
                            tags.value =
                                queryEngine.getTags(image.tags.map { it.id })
                            Log.v(TAG, "Got ${tags.value?.size} tags")
                        }
                        if (image.performers.isNotEmpty()) {
                            performers.value =
                                queryEngine.findPerformers(performerIds = image.performers.map { it.id })
                        }
                    } else {
                        loadingState.value = ImageLoadingState.Error
                    }
                } catch (ex: Exception) {
                    loadingState.value = ImageLoadingState.Error
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
        val ids = tags.value?.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                val mutationEngine = MutationEngine(server!!)
                val result = mutationEngine.updateImage(imageId = imageId, tagIds = mutable)
                if (result != null) {
                    tags.value = result.tags.map { it.tagData }
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
        val perfs = performers.value?.map { it.id }
        perfs?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                val mutationEngine = MutationEngine(server!!)
                val result = mutationEngine.updateImage(imageId = imageId, performerIds = mutable)
                if (result != null) {
                    performers.value = result.performers.map { it.performerData }
                }
            }
        }
    }

    fun updateRating(
        imageId: String,
        rating100: Int,
    ) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val mutationEngine = MutationEngine(server!!)
            val newRating =
                mutationEngine.updateImage(imageId, rating100 = rating100)?.rating100 ?: 0
            this@ImageDetailsViewModel.rating100.value = newRating
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }

    fun updateOCount(action: suspend MutationEngine.(String) -> OCounter) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val mutationEngine = MutationEngine(server!!)
            val newOCount = action.invoke(mutationEngine, _image.value!!.id)
            oCount.value = newOCount.count
        }
    }

    private var slideshowJob: Job? = null

    fun startSlideshow() {
        _slideshow.value = true
        _slideshowPaused.value = false
        if (_image.value?.isImageClip == false) {
            pulseSlideshow()
        }
    }

    fun stopSlideshow() {
        slideshowJob?.cancel()
        _slideshow.value = false
    }

    fun pauseSlideshow() {
        if (_slideshow.value == true) {
            Log.v(TAG, "pauseSlideshow")
            _slideshowPaused.value = true
            slideshowJob?.cancel()
        }
    }

    fun unpauseSlideshow() {
        if (_slideshow.value == true) {
            Log.v(TAG, "unpauseSlideshow")
            _slideshowPaused.value = false
        }
    }

    fun pulseSlideshow() = pulseSlideshow(slideshowDelay)

    fun pulseSlideshow(milliseconds: Long) {
        Log.v(TAG, "pulseSlideshow $milliseconds")
        slideshowJob?.cancel()
        if (slideshow.value!!) {
            slideshowJob =
                viewModelScope
                    .launch(StashCoroutineExceptionHandler()) {
                        delay(milliseconds)
                        Log.v(TAG, "pulseSlideshow after delay")
                        if (slideshowActive.value == true) {
                            nextImage()
                        }
                    }.apply {
                        invokeOnCompletion { if (it !is CancellationException) pulseSlideshow() }
                    }
        }
    }

    fun updateImageFilter(newFilter: VideoFilter) {
        _imageFilter.value = newFilter
    }

    fun saveImageFilter() {
        if (server != null) {
            image.value?.let {
                viewModelScope.launchIO(StashCoroutineExceptionHandler()) {
                    val vf = _imageFilter.value
                    if (vf != null) {
                        StashApplication
                            .getDatabase()
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
