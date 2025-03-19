package com.github.damontecres.stashapp.image

import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.data.ThrottledLiveData
import com.github.damontecres.stashapp.image.ImageFragment.Companion.isDown
import com.github.damontecres.stashapp.image.ImageFragment.Companion.isLeft
import com.github.damontecres.stashapp.image.ImageFragment.Companion.isRight
import com.github.damontecres.stashapp.image.ImageFragment.Companion.isUp
import com.github.damontecres.stashapp.playback.VideoFilterViewModel
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.height
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.width
import com.github.damontecres.stashapp.views.StashZoomImageView
import com.github.damontecres.stashapp.views.models.ImageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Display an image
 */
class ImageViewFragment :
    Fragment(R.layout.image_layout),
    ImageController {
    private val viewModel: ImageViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val filterViewModel: VideoFilterViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var mainImage: StashZoomImageView

    private val duration = 200L
    private var duringAnimation = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        mainImage = view.findViewById(R.id.image_view_image)
        viewModel.imageController = this
        viewModel.image.observe(viewLifecycleOwner) { newImage ->
            filterViewModel.videoFilter.value = null
            if (!newImage.isImageClip) {
                loadImage(newImage)
            }
        }
        ThrottledLiveData(
            filterViewModel.videoFilter,
            500L,
        ).observe(viewLifecycleOwner) { videoFilter ->
            if (videoFilter != null && videoFilter.hasImageFilter()) {
                mainImage.colorFilter = ColorMatrixColorFilter(videoFilter.createColorMatrix())
            } else {
                mainImage.clearColorFilter()
            }
        }
    }

    private fun loadImage(image: ImageData) {
        reset(false)
//        mainImage.setImageDrawable(null)

        val placeholder =
            object : CircularProgressDrawable(requireContext()) {
                // ZoomImageView requires that drawables have an intrinsic height/width
                // So override it here to be the size of the view since the default implementation is -1
                override fun getIntrinsicHeight(): Int = 250

                override fun getIntrinsicWidth(): Int = 250
            }
        placeholder.strokeWidth = 3f
        placeholder.centerRadius = 12f
        placeholder.setColorSchemeColors(requireContext().getColor(R.color.selected_background))

        val imageUrl = image.paths.image
        if (imageUrl != null) {
            val job =
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler() + Dispatchers.IO) {
                    delay(300)
                    withContext(Dispatchers.Main) {
                        placeholder.start()
                    }
                }
            val factory =
                DrawableCrossFadeFactory
                    .Builder(300)
                    .setCrossFadeEnabled(true)
                    .build()
            val imageLoader =
                StashGlide
                    .withCaching(requireContext(), imageUrl)
                    .transition(withCrossFade(factory))
                    .placeholder(placeholder)
                    .thumbnail(
                        image.paths.thumbnail?.let { StashGlide.withCaching(requireContext(), it) },
                    ).listener(
                        object : RequestListener<Drawable?> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable?>,
                                isFirstResource: Boolean,
                            ): Boolean {
                                Log.e(TAG, "onLoadFailed for ${image.id}", e)
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "Error loading ${image.titleOrFilename}!",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                viewModel.pulseSlideshow()
                                job.cancel()
                                return true
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable?>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean,
                            ): Boolean {
                                viewModel.pulseSlideshow()
                                job.cancel()
                                return false
                            }
                        },
                    )
            filterViewModel.maybeGetSavedFilter()
            // TODO figure out how to apply the effects on load without double applying
//            if (filterViewModel.saveVideoFilter) {
//                // If saving filters, check if one exists
//                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(true)) {
//                    val vf = filterViewModel.getSavedFilter()?.videoFilter
//                    if (vf != null && vf.hasImageFilter()) {
//                        imageLoader
//                            .transform(EffectTransformation(vf))
//                            .into(mainImage)
//                    } else {
//                        imageLoader.into(mainImage)
//                    }
//                }
//            } else {
//                // Not saving filters, so just directly load the image
//                imageLoader.into(mainImage)
//            }
            imageLoader.into(mainImage)
        }
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isImageZoomedIn()) {
            // Image is zoomed in
            // TODO maybe pan faster on long presses?
            val panDistance =
                mainImage.drawable.intrinsicWidth.coerceAtMost(mainImage.drawable.intrinsicHeight) * .05f
            if (ImageFragment.isDirectionalDpadKey(event.keyCode)) {
                val rotation = abs(mainImage.rotation).toInt()
                var panX = 0f
                var panY = 0f
                if (isLeft(event.keyCode)) {
                    panX = panDistance
                } else if (isRight(event.keyCode)) {
                    panX = -panDistance
                } else if (isUp(event.keyCode)) {
                    panY = panDistance
                } else if (isDown(event.keyCode)) {
                    panY = -panDistance
                } else {
                    // Should never occur
                    throw IllegalStateException()
                }
                when (rotation) {
                    0 -> {}
                    90 ->
                        (panX to panY).apply {
                            panX = second
                            panY = -first
                        }

                    180 -> {
                        panX = -panX
                        panY = -panY
                    }

                    270 ->
                        (panX to panY).apply {
                            panX = -second
                            panY = first
                        }
                }
                panX *= mainImage.scaleX
                mainImage.panBy(panX, panY, true)
                viewModel.pulseSlideshow()
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
            Log.v(TAG, "scale=$scale")
            val flipY = if (mainImage.scaleY < 0) -1f else 1f
            val flipX = if (mainImage.scaleX < 0) -1f else 1f

            mainImage
                .animate()
                .rotationBy(rotation)
                .setDuration(duration)
                .scaleX(scale * flipX)
                .scaleY(scale * flipY)
                .withEndAction {
                    duringAnimation = false
                }.start()
        }
    }

    private fun isImageRotated(): Boolean {
        val rotation = abs(mainImage.rotation)
        return rotation == 90f || rotation == 270f
    }

    private fun calculateRotationScale(): Float {
        val image = viewModel.image.value!!

        // Adapted from https://github.com/stashapp/stash/blob/v0.26.2/ui/v2.5/src/components/Scenes/SceneDetails/SceneVideoFilterPanel.tsx#L529
        val imageWidth =
            image.visual_files
                .first()
                .width!!
                .toFloat()
        val imageHeight =
            image.visual_files
                .first()
                .height!!
                .toFloat()
        val imageAspectRatio = imageWidth / imageHeight
        val imageNewAspectRatio = imageHeight / imageWidth

        val viewWidth = mainImage.width.toFloat()
        val viewHeight = mainImage.height.toFloat()
        val viewAspectRation = viewWidth / viewHeight

        // rs > ri ? (wi * hs/hi, hs) : (ws, hi * ws/wi)
        // Determine if video is currently constrained by view height or width.
        val scaledImageHeight: Float
        val scaledImageWidth: Float
        if (viewAspectRation > imageAspectRatio) {
            // Image has it's width scaled
            // Image is constrained by it's height
            scaledImageHeight = viewHeight
            scaledImageWidth = (viewHeight / imageHeight) * imageWidth
        } else {
            // Image has it's height scaled
            // Image is constrained by it's width
            scaledImageWidth = viewWidth
            scaledImageHeight = (viewWidth / imageWidth) * imageHeight
        }

        // but now the image is rotated
        val scaleFactor =
            if (viewAspectRation > imageNewAspectRatio) {
                // Rotated image will be constrained by it's height
                // so we need to scaledImageWidth to match the view height
                viewHeight / scaledImageWidth
            } else {
                // Rotated image will be constrained by it's width
                // so we need to scaledImageHeight to match the view width
                viewWidth / scaledImageHeight
            }
        return scaleFactor
    }

    override fun isImageZoomedIn(): Boolean = (mainImage.zoom * 100).toInt() > 100

    override fun flip() {
        if (!duringAnimation) {
            duringAnimation = true
            val animator =
                mainImage
                    .animate()
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
        viewModel.pulseSlideshow()
    }

    override fun reset(animate: Boolean) {
        if (animate) {
            if (!duringAnimation) {
                resetZoom()
                duringAnimation = true
                mainImage
                    .animate()
                    .rotation(0f)
                    .setDuration(duration)
                    .scaleX(1f)
                    .scaleY(1f)
                    .withEndAction {
                        duringAnimation = false
                    }.start()
            }
        } else {
            viewModel.pulseSlideshow()
            mainImage.cancelAnimations()
            mainImage.moveTo(1f, 0f, 0f, false)
            mainImage.rotation = 0f
            mainImage.scaleX = 1f
            mainImage.scaleY = 1f
        }
    }

    companion object {
        private const val TAG = "ImageViewFragment"
    }
}
