package com.github.damontecres.stashapp.image

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.api.fragment.ImageData

/**
 * The [ViewModel] for [ImageActivity] and its fragments
 */
class ImageViewModel(private val state: SavedStateHandle) : ViewModel() {
    var imageController: ImageController? = null
    var videoController: VideoController? = null

    private val _image = MutableLiveData<ImageData>()

    /**
     * The current image, most users will observe this for changes
     */
    val image: LiveData<ImageData> = _image

    val imageId: LiveData<String> = state.getLiveData("imageId")

    /**
     * Set the current image to a new one
     */
    fun setImage(newImage: ImageData) {
        _image.value = newImage
        state["imageId"] = newImage.id
    }
}
