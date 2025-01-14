package com.github.damontecres.stashapp.views.models

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
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
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

/**
 * The [ViewModel] for images
 */
class ImageViewModel(
    private val state: SavedStateHandle,
) : ViewModel() {
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
    val slideshow = MutableLiveData(false)

    val currentPosition = MutableLiveData(-1)
    val totalCount = MutableLiveData(-1)

    /**
     * Set the current image to a new one
     */
    fun setImage(newImage: ImageData) {
        _image.value = newImage
        state["imageId"] = newImage.id
    }

    fun init(slideshow: Destination.Slideshow) {
        currentPosition.value = slideshow.position
        createPager(slideshow.filterArgs)

        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            totalCount.value = pager.source.getCount()
            switchImage(slideshow.position, false)
            this@ImageViewModel.slideshow.value = slideshow.automatic
        }
    }

    private fun createPager(filterArgs: FilterArgs) {
        val dataSupplier =
            DataSupplierFactory(
                StashServer.getCurrentServerVersion(),
            ).create<FindImagesQuery.Data, ImageData, CountImagesQuery.Data>(
                filterArgs,
            ) as ImageDataSupplier
        val pagingSource =
            StashPagingSource<FindImagesQuery.Data, ImageData, ImageData, CountImagesQuery.Data>(
                QueryEngine(StashServer.requireCurrentServer()),
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

    fun switchImage(
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
        } else if (showToast) {
            Toast
                .makeText(
                    StashApplication.getApplication(),
                    "Already at beginning",
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    companion object {
        private const val TAG = "ImageViewModel"
    }
}
