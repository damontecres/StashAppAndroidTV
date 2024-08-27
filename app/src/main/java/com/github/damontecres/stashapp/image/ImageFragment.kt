package com.github.damontecres.stashapp.image

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.damontecres.stashapp.ImageActivity.Companion.TAG
import com.github.damontecres.stashapp.ImageActivity.Companion.isDirectionalDpadKey
import com.github.damontecres.stashapp.ImageActivity.Companion.isDown
import com.github.damontecres.stashapp.ImageActivity.Companion.isLeft
import com.github.damontecres.stashapp.ImageActivity.Companion.isRight
import com.github.damontecres.stashapp.ImageActivity.Companion.isUp
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.height
import com.github.damontecres.stashapp.util.isGif
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.maxFileSize
import com.github.damontecres.stashapp.util.width
import com.otaliastudios.zoom.ZoomImageView
import kotlin.math.abs

class ImageFragment : Fragment(R.layout.image_layout), ImageController {
    private val viewModel: ImageViewModel by activityViewModels<ImageViewModel>()

    private lateinit var parentView: View
    private lateinit var mainImage: ZoomImageView

    private val duration = 200L
    private var duringAnimation = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        parentView = view
        mainImage = view.findViewById<ZoomImageView>(R.id.image_view_image)

        viewModel.image.observe(viewLifecycleOwner) { newImage ->
            if (!newImage.isImageClip) {
                loadImage(newImage)
            }
        }
        viewModel.imageController = this
    }

    private fun loadImage(image: ImageData) {
        mainImage.setImageDrawable(null)
        reset()

        // TODO gifs don't display on first view
        // TODO gifs display along left side if scrolled to
        if (image.isGif) {
            Log.v(TAG, "Image ${image.id} is a gif")
            mainImage.post {
                // Properly scale the image after layout
                val imageFile = image.visual_files.first()
                val width = imageFile.width!!
                val height = imageFile.height!!

                val scale =
                    Math.min(
                        mainImage.height.toDouble() / height,
                        mainImage.width.toDouble() / width,
                    )

                val targetHeight = height * scale
                val targetWidth = width * scale

                val lp = mainImage.layoutParams
                lp.width = targetWidth.toInt()
                lp.height = targetHeight.toInt()
                mainImage.layoutParams = lp
            }
        }

        // TODO
        val placeholder =
            object : CircularProgressDrawable(requireContext()) {
                // ZoomImageView requires that drawables have an intrinsic height/width
                // So override it here to be the size of the view since the default implementation is -1
                override fun getIntrinsicHeight(): Int {
                    return mainImage.height
                }

                override fun getIntrinsicWidth(): Int {
                    return mainImage.width
                }
            }
        placeholder.setStyle(CircularProgressDrawable.LARGE)
        placeholder.setColorSchemeColors(requireContext().getColor(R.color.selected_background))
        placeholder.start()

        val imageUrl = image.paths.image
        if (imageUrl != null) {
            StashGlide.with(requireContext(), imageUrl, image.maxFileSize)
                // TODO
//                .placeholder(placeholder)
                .transition(withCrossFade())
                .listener(
                    object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean,
                        ): Boolean {
                            Log.v(TAG, "onLoadFailed for $imageUrl")
                            Toast.makeText(
                                requireContext(),
                                "Image loading failed!",
                                Toast.LENGTH_LONG,
                            ).show()
                            return true
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable?>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean,
                        ): Boolean {
                            return false
                        }
                    },
                )
                .into(mainImage)
        }
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isImageZoomedIn()) {
            // Image is zoomed in
            val panDistance =
                mainImage.drawable.intrinsicWidth.coerceAtMost(mainImage.drawable.intrinsicHeight) * .05f
            if (isDirectionalDpadKey(event.keyCode)) {
                if (isLeft(event.keyCode)) {
                    mainImage.panBy(panDistance, 0f, true)
                } else if (isRight(event.keyCode)) {
                    mainImage.panBy(-panDistance, 0f, true)
                } else if (isUp(event.keyCode)) {
                    mainImage.panBy(0f, panDistance, true)
                } else if (isDown(event.keyCode)) {
                    mainImage.panBy(0f, -panDistance, true)
                } else {
                    // Should never occur
                    throw IllegalStateException()
                }
                return true
            }
        }
        return false
    }

    override fun zoomIn() {
        mainImage.zoomIn()
    }

    override fun zoomOut() {
        mainImage.zoomOut()
    }

    override fun rotateLeft() {
        rotate(-90f)
    }

    override fun rotateRight() {
        rotate(90f)
    }

    private fun rotate(rotation: Float) {
        if (!duringAnimation) {
            duringAnimation = true

            val scale = if (isImageRotated()) 1f else calculateRotationScale()
            val flipY = if (mainImage.scaleY < 0) -1f else 1f
            val flipX = if (mainImage.scaleX < 0) -1f else 1f

            mainImage.animate()
                .rotationBy(rotation)
                .setDuration(duration)
                .scaleX(scale * flipX)
                .scaleY(scale * flipY)
                .withEndAction {
                    duringAnimation = false
                }
                .start()
        }
    }

    private fun isImageRotated(): Boolean {
        val rotation = abs(mainImage.rotation)
        return rotation == 90f || rotation == 270f
    }

    private fun calculateRotationScale(): Float {
        val image = viewModel.image.value!!

        // TODO the view height/width are half what they should be
        val viewHeight = mainImage.height.toFloat()
        val viewWidth = mainImage.width.toFloat()
        val imageHeight = image.visual_files.first().height!!.toFloat()
        val imageWidth = image.visual_files.first().width!!.toFloat()
        val imageHeightPx = mainImage.drawable.intrinsicHeight
        val imageWidthPx = mainImage.drawable.intrinsicWidth

        Log.v(
            TAG,
            "viewWidth=$viewWidth, viewHeight=$viewHeight\n" +
                "imageWidth=$imageWidth, imageHeight=$imageHeight\n" +
                "imageWidthPx=$imageWidthPx, imageHeightPx=$imageHeightPx\n",
        )

        return if (imageWidthPx >= viewHeight) {
            // Need to scale the image width down
            Log.v(TAG, "Scaling image width down to view height")
            viewHeight / imageWidthPx
        } else if (imageHeightPx >= viewWidth) {
            // Need to scale the image height down
            Log.v(TAG, "Scaling image height down to view width")
            viewWidth / imageHeightPx
        } else {
            // Need to scale the image up
            val ratio = viewHeight / imageWidthPx
            val ratio2 = viewWidth / imageHeightPx
            if (ratio > ratio2) {
                Log.v(TAG, "Scaling image width up to view height")
                ratio2
            } else {
                Log.v(TAG, "Scaling image height up to view width")
                ratio
            }
        }
    }

    override fun isImageZoomedIn(): Boolean {
        return (mainImage.zoom * 100).toInt() > 100
    }

    override fun flip() {
        if (!duringAnimation) {
            duringAnimation = true
            val animator =
                mainImage.animate()
                    .setDuration(duration)
                    .withEndAction {
                        duringAnimation = false
                    }
            if (isImageRotated()) {
                animator.scaleY(mainImage.scaleY * -1)
            } else {
                animator.scaleX(mainImage.scaleX * -1)
            }
            animator.start()
        }
    }

    fun resetZoom() {
        mainImage.zoomTo(1.0f, true)
    }

    override fun reset() {
        if (!duringAnimation) {
            resetZoom()
            duringAnimation = true
            mainImage.animate()
                .rotation(0f)
                .setDuration(duration)
                .scaleX(1f)
                .scaleY(1f)
                .withEndAction {
                    duringAnimation = false
                }
                .start()
        }
    }
}
