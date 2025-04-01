package com.github.damontecres.stashapp.views.models

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.CountImagesQuery
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.image.ImageController
import com.github.damontecres.stashapp.image.VideoController
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.suppliers.StashSparseFilterFetcher
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isImageClip
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The [ViewModel] for images
 */
class ImageViewModel(
    private val state: SavedStateHandle,
) : ViewModel() {
    private lateinit var server: StashServer
    var imageController: ImageController? = null
    var videoController: VideoController? = null
    private lateinit var pager: StashSparseFilterFetcher<FindImagesQuery.Data, ImageData>

    private val _image = MutableLiveData<ImageData>()

    /**
     * The current image, most users will observe this for changes
     */
    val image: LiveData<ImageData> = _image

    val imageId: LiveData<String> = state.getLiveData("imageId")

    /**
     * Whether the slideshow is running or not
     */
    private val _slideshow = MutableLiveData(false)
    val slideshow: LiveData<Boolean> = _slideshow
    private val _slideshowPaused = MutableLiveData(false)
    val slideshowPaused: LiveData<Boolean> = _slideshowPaused

    val slideshowActive =
        slideshow
            .asFlow()
            .combine(slideshowPaused.asFlow()) { slideshow, paused ->
                slideshow && !paused
            }.asLiveData()

    val slideshowDelay =
        PreferenceManager.getDefaultSharedPreferences(StashApplication.getApplication()).getInt(
            StashApplication.getApplication().getString(R.string.pref_key_slideshow_duration),
            StashApplication.getApplication().resources.getInteger(R.integer.pref_key_slideshow_duration_default),
        ) * 1000L

    val currentPosition = MutableLiveData(-1)
    val totalCount = MutableLiveData(-1)

    /**
     * Set the current image to a new one
     */
    private fun setImage(newImage: ImageData) {
        _image.value = newImage
        state["imageId"] = newImage.id
    }

    fun init(
        server: StashServer,
        slideshow: Destination.Slideshow,
    ) {
        this.server = server
        currentPosition.value = slideshow.position
        createPager(server, slideshow.filterArgs)

        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            totalCount.value = pager.source.getCount()
            switchImage(slideshow.position, false)
            if (slideshow.automatic) {
                startSlideshow()
            }
        }
    }

    private fun createPager(
        server: StashServer,
        filterArgs: FilterArgs,
    ) {
        val dataSupplier =
            DataSupplierFactory(
                server.version,
            ).create<FindImagesQuery.Data, ImageData, CountImagesQuery.Data>(
                filterArgs,
            ) as ImageDataSupplier
        val pagingSource =
            StashPagingSource<FindImagesQuery.Data, ImageData, ImageData, CountImagesQuery.Data>(
                QueryEngine(server),
                dataSupplier,
            )
        val pageSize =
            PreferenceManager
                .getDefaultSharedPreferences(StashApplication.getApplication())
                .getInt("maxSearchResults", 25)
        pager = StashSparseFilterFetcher(pagingSource, pageSize)
    }

    fun nextImage(causedByUser: Boolean) {
        val curr = currentPosition.value
        if (curr != null) {
            switchImage(curr + 1, causedByUser)
        }
    }

    fun previousImage(causedByUser: Boolean) {
        val curr = currentPosition.value
        if (curr != null) {
            switchImage(curr - 1, causedByUser)
        }
    }

    private fun switchImage(
        newPosition: Int,
        showToast: Boolean = true,
    ) {
        Log.v(TAG, "switchImage ${currentPosition.value} => $newPosition")
        val totalCount = this.totalCount.value

        if (totalCount != null && newPosition > totalCount) {
            if (showToast) {
                Toast
                    .makeText(
                        StashApplication.getApplication(),
                        "No more images",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        } else if (newPosition >= 0) {
            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                val image = pager.get(newPosition)
                if (image != null && image.paths.image != null) {
                    currentPosition.value = newPosition
                    setImage(image)
                } else if (image == null) {
                    if (showToast) {
                        Toast
                            .makeText(
                                StashApplication.getApplication(),
                                "No more images",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }
            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                listOf(newPosition + 1, newPosition - 1).forEach { position ->
                    val image = pager.get(position)
                    if (image?.isImageClip == false) {
                        image.paths.image?.let {
                            Log.v(TAG, "Preloading ${image.id}")
                            StashGlide
                                .withCaching(StashApplication.getApplication(), it)
                                .preload()
                        }
                    }
                }
            }
        } else if (showToast) {
            Toast
                .makeText(
                    StashApplication.getApplication(),
                    "Already at beginning",
                    Toast.LENGTH_SHORT,
                ).show()
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
            _slideshowPaused.value = true
            slideshowJob?.cancel()
        }
    }

    fun unpauseSlideshow() {
        if (_slideshow.value == true) {
            _slideshowPaused.value = false
        }
    }

    fun pulseSlideshow(milliseconds: Long = slideshowDelay) {
        slideshowJob?.cancel()
        if (slideshow.value!!) {
            slideshowJob =
                viewModelScope
                    .launch(StashCoroutineExceptionHandler()) {
                        delay(milliseconds)
                        if (slideshowActive.value == true) {
                            nextImage(false)
                        }
                    }.apply {
                        invokeOnCompletion { if (it !is CancellationException) pulseSlideshow() }
                    }
        }
    }

    fun tearDownSlideshow() {
        slideshowJob?.cancel()
    }

    companion object {
        private const val TAG = "ImageViewModel"
    }
}
