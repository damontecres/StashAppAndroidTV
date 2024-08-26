package com.github.damontecres.stashapp.image

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.api.fragment.ImageData

class ImageViewModel : ViewModel() {
    var imageController: ImageController? = null

    private val _image = MutableLiveData<ImageData>()
    val image: LiveData<ImageData> = _image

    fun setImage(newImage: ImageData) {
        _image.value = newImage
    }
}
