package com.github.damontecres.stashapp.views

import android.content.Intent
import android.util.Log
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.StashGridFragment
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.util.maxFileSize
import com.github.damontecres.stashapp.util.putFilterArgs
import com.github.damontecres.stashapp.views.ClassOnItemViewClickedListener.SimpleOnItemViewClickedListener

/**
 * A [SimpleOnItemViewClickedListener] for images in a [StashGridFragment] to allow for scrolling through the grid's other images
 */
class ImageGridClickedListener(val fragment: StashGridFragment) :
    SimpleOnItemViewClickedListener<ImageData> {
    override fun onItemClicked(item: ImageData) {
        val position = fragment.currentSelectedPosition
        Log.v(TAG, "position=$position")
        val intent = Intent(fragment.requireActivity(), ImageActivity::class.java)
        intent.putExtra(ImageActivity.INTENT_IMAGE_ID, item.id)
        intent.putExtra(ImageActivity.INTENT_IMAGE_URL, item.paths.image)
        intent.putExtra(ImageActivity.INTENT_IMAGE_SIZE, item.maxFileSize)
        intent.putExtra(ImageActivity.INTENT_POSITION, position)
        intent.putFilterArgs(ImageActivity.INTENT_FILTER_ARGS, fragment.filterArgs)
        fragment.requireActivity().startActivity(intent)
    }

    companion object {
        private const val TAG = "ImageGridClickedListener"
    }
}
