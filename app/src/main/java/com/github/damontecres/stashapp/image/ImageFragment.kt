package com.github.damontecres.stashapp.image

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.playback.PlaybackVideoFiltersFragment
import com.github.damontecres.stashapp.playback.VideoFilterViewModel
import com.github.damontecres.stashapp.util.DefaultKeyEventCallback
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.views.models.ImageViewModel
import java.util.Timer

class ImageFragment :
    Fragment(R.layout.image_fragment),
    DefaultKeyEventCallback {
    private val viewModel: ImageViewModel by viewModels()
    private val filterViewModel: VideoFilterViewModel by viewModels()

    private var timer: Timer? = null

    private val imageViewFragment = ImageViewFragment()
    private val imageClipFragment = ImageClipFragment()
    private val imageDetailsFragment = ImageDetailsFragment()
    private val videoFiltersFragment = PlaybackVideoFiltersFragment()

    private val overlayFragment = imageDetailsFragment

    private val overlayIsVisible: Boolean
        get() = !overlayFragment.isHidden

    private val filterOverlayIsVisible: Boolean
        get() = videoFiltersFragment.isAdded && !videoFiltersFragment.isHidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val slideshow = requireArguments().getDestination<Destination.Slideshow>()
        viewModel.init(slideshow)
        filterViewModel.init(DataType.IMAGE) {
            viewModel.image.value!!.id
        }

        childFragmentManager.commit {
            listOf(
                imageViewFragment,
                imageClipFragment,
                imageDetailsFragment,
            ).forEach {
                add(R.id.root, it, it::class.java.simpleName)
                hide(it)
            }
        }

        viewModel.image.observe(this) { newImage ->
            Log.v(TAG, "newImage: id=${newImage.id}")
            childFragmentManager.commit {
                // TODO switch to sliding transitions?
                setCustomAnimations(
                    R.animator.fade_in,
                    R.animator.fade_out,
                    R.animator.fade_in,
                    R.animator.fade_out,
                )
                if (newImage.isImageClip) {
                    hide(imageViewFragment)
                    show(imageClipFragment)
                } else {
                    hide(imageClipFragment)
                    show(imageViewFragment)
                }
                hide(overlayFragment)
            }
        }

//        val delay =
//            PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt(
//                getString(R.string.pref_key_slideshow_duration),
//                resources.getInteger(R.integer.pref_key_slideshow_duration_default),
//            ) * 1000L
//        viewModel.slideshow.observe(this) { newValue ->
//            timer?.cancel()
//            if (newValue) {
//                Log.i(TAG, "Setting up slideshow timer")
//                timer =
//                    kotlin.concurrent.timer(
//                        name = "imageSlideshow",
//                        daemon = true,
//                        initialDelay = delay,
//                        period = delay,
//                    ) {
//                        if (!overlayIsVisible) {
//                            viewModel.nextImage(false)
//                        }
//                    }
//            }
//        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.tearDownSlideshow()
    }

    private fun showOverlay() {
        viewModel.pulseSlideshow(Long.MAX_VALUE)
        childFragmentManager.commitNow {
            setCustomAnimations(
                androidx.leanback.R.anim.abc_slide_in_bottom,
                androidx.leanback.R.anim.abc_slide_out_bottom,
            )
            show(overlayFragment)
        }
        overlayFragment.requestFocus()
    }

    fun hideOverlay() {
        if (overlayIsVisible) {
            childFragmentManager.commit {
                setCustomAnimations(
                    androidx.leanback.R.anim.abc_slide_in_bottom,
                    androidx.leanback.R.anim.abc_slide_out_bottom,
                )
                hide(overlayFragment)
            }
            if (viewModel.image.value?.isImageClip == false) {
                viewModel.pulseSlideshow()
            }
        }
    }

    fun showFilterOverlay() {
        hideOverlay()
        viewModel.pulseSlideshow(Long.MAX_VALUE)
        childFragmentManager.commitNow {
            setCustomAnimations(
                androidx.leanback.R.anim.abc_slide_in_top,
                androidx.leanback.R.anim.abc_slide_out_top,
            )
            if (videoFiltersFragment.isAdded) {
                show(videoFiltersFragment)
            } else {
                add(R.id.root, videoFiltersFragment)
            }
        }
        videoFiltersFragment.view?.requestFocus()
    }

    fun hideFilterOverlay() {
        if (filterOverlayIsVisible) {
            childFragmentManager.commit {
                setCustomAnimations(
                    androidx.leanback.R.anim.abc_slide_in_top,
                    androidx.leanback.R.anim.abc_slide_out_top,
                )
                hide(videoFiltersFragment)
            }
            if (viewModel.image.value?.isImageClip == false) {
                viewModel.pulseSlideshow()
            }
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (!isAdded) {
            return false
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (overlayIsVisible) {
                hideOverlay()
                return true
            } else if (filterOverlayIsVisible) {
                hideFilterOverlay()
                return true
            } else if (imageViewFragment.isImageZoomedIn()) {
                imageViewFragment.resetZoom()
                return true
            }
        } else if (filterOverlayIsVisible) {
            return false
        } else if (isDpadKey(keyCode)) {
            if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) && !overlayIsVisible) {
                showOverlay()
                return true
            } else if (!overlayIsVisible && imageViewFragment.isImageZoomedIn()) {
                return imageViewFragment.dispatchKeyEvent(event)
            } else if (!overlayIsVisible && !imageViewFragment.isImageZoomedIn()) {
                // Overlay is not showing and the image is not zoomed in
                // So maybe move to another image if left or right
                if (isLeft(event.keyCode)) {
                    viewModel.previousImage(true)
                    return true
                } else if (isRight(event.keyCode)) {
                    viewModel.nextImage(true)
                    return true
                }
            }
        }
        if (event.keyCode != KeyEvent.KEYCODE_BACK && !overlayIsVisible) {
            showOverlay()
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "ImageFragment"

        fun isDpadKey(keyCode: Int): Boolean =
            isDirectionalDpadKey(keyCode) ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER

        fun isDirectionalDpadKey(keyCode: Int): Boolean =
            keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (
                    keyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT
                )

        fun isLeft(keyCode: Int): Boolean =
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT
                )

        fun isRight(keyCode: Int): Boolean =
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT
                )

        fun isUp(keyCode: Int): Boolean =
            keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (
                    keyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT
                )

        fun isDown(keyCode: Int): Boolean =
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT
                )
    }
}
