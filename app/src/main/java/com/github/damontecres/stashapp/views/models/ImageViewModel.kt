package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.image.ImageController
import com.github.damontecres.stashapp.image.VideoController

/**
 * The [ViewModel] for [com.github.damontecres.stashapp.ImageActivity] and its fragments
 */
class ImageViewModel(
    private val state: SavedStateHandle,
) : ViewModel() {
    var imageController: ImageController? = null
    var videoController: VideoController? = null

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

    /**
     * Set the current image to a new one
     */
    fun setImage(newImage: ImageData) {
        _image.value = newImage
        state["imageId"] = newImage.id
    }
}
