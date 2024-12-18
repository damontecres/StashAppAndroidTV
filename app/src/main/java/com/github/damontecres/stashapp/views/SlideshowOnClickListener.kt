package com.github.damontecres.stashapp.views

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.View.OnClickListener
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.maxFileSize
import com.github.damontecres.stashapp.util.putFilterArgs

class SlideshowOnClickListener(
    private val context: Context,
    private val getImageAndFilter: () -> ImageAndFilter,
) : OnClickListener {
    override fun onClick(v: View?) {
        val intent = Intent(context, ImageActivity::class.java)
        val (position, item, filterArgs) = getImageAndFilter()
        if (item != null) {
            intent
                .putExtra(ImageActivity.INTENT_IMAGE_ID, item.id)
                .putExtra(ImageActivity.INTENT_IMAGE_URL, item.paths.image)
                .putExtra(ImageActivity.INTENT_IMAGE_SIZE, item.maxFileSize)
                .putExtra(ImageActivity.INTENT_POSITION, position)
                .putFilterArgs(ImageActivity.INTENT_FILTER_ARGS, filterArgs)
                .putExtra(ImageActivity.INTENT_IMAGE_SLIDESHOW, true)
            context.startActivity(intent)
        }
    }
}

data class ImageAndFilter(
    val position: Int,
    val image: ImageData?,
    val filter: FilterArgs,
)
