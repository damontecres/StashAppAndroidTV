package com.github.damontecres.stashapp.itemclick

import android.content.Context
import android.content.Intent
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.StashGridFragment
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.views.ClassOnItemViewClickedListener.SimpleOnItemViewClickedListener

class ImageGridClickedListener(
    val context: Context,
    val fragment: StashGridFragment<FindImagesQuery.Data, ImageData>,
    val intentCallBack: (Intent) -> Unit,
) :
    SimpleOnItemViewClickedListener<ImageData> {
    override fun onItemClicked(item: ImageData) {
        val snapshot = fragment.pagingAdapter.snapshot()
        val position = snapshot.indexOf(item)
        val intent = Intent(context, ImageActivity::class.java)
        intent.putExtra(ImageActivity.INTENT_IMAGE_ID, item.id)
        intent.putExtra(ImageActivity.INTENT_IMAGE_URL, item.paths.image)
        intent.putExtra(
            ImageActivity.INTENT_IMAGE_SIZE,
            item.visual_files.maxOfOrNull {
                it.onBaseFile?.size?.toString()?.toInt() ?: -1
            } ?: -1,
        )
        intent.putExtra(ImageActivity.INTENT_POSITION, position)
        intentCallBack(intent)
        context.startActivity(intent)
    }
}
