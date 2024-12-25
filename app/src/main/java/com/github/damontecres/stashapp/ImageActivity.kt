package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.CountImagesQuery
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.image.ImageClipFragment
import com.github.damontecres.stashapp.image.ImageDetailsFragment
import com.github.damontecres.stashapp.image.ImageViewFragment
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.suppliers.StashSparseFilterFetcher
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getFilterArgs
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.views.models.ImageViewModel
import kotlinx.coroutines.launch
import java.util.Timer

class ImageActivity : FragmentActivity(R.layout.activity_image) {
    private var timer: Timer? = null
    private val viewModel: ImageViewModel by viewModels<ImageViewModel>()

    private lateinit var pager: StashSparseFilterFetcher<FindImagesQuery.Data, ImageData>

    private var currentPosition = -1
    private var totalCount: Int? = null

    private var canScrollImages = false

    private val imageViewFragment = ImageViewFragment()
    private val imageClipFragment = ImageClipFragment()
    private val imageDetailsFragment = ImageDetailsFragment()

    private val overlayFragment = imageDetailsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.slideshow.value = intent.getBooleanExtra(INTENT_IMAGE_SLIDESHOW, false)

        lifecycleScope.launch(StashCoroutineExceptionHandler()) {
            val imageId = intent.getStringExtra(INTENT_IMAGE_ID)!!

            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            val image = queryEngine.getImage(imageId)!!
            viewModel.setImage(image)

            val pageSize =
                PreferenceManager
                    .getDefaultSharedPreferences(this@ImageActivity)
                    .getInt("maxSearchResults", 25)

            currentPosition = intent.getIntExtra(INTENT_POSITION, -1)

            val dataSupplier = createDataSupplier()
            if (dataSupplier != null) {
                val pagingSource =
                    StashPagingSource<FindImagesQuery.Data, ImageData, ImageData, CountImagesQuery.Data>(
                        queryEngine,
                        dataSupplier,
                    )
                pager = StashSparseFilterFetcher(pagingSource, pageSize)
                totalCount = pagingSource.getCount()
                canScrollImages = true
            }
        }

        supportFragmentManager.commit {
            listOf(imageViewFragment, imageClipFragment, imageDetailsFragment).forEach {
                add(android.R.id.content, it, it::class.java.simpleName)
                hide(it)
            }
        }

        viewModel.image.observe(this) { newImage ->
            supportFragmentManager.commit {
                setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
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

        val delay =
            PreferenceManager.getDefaultSharedPreferences(this).getInt(
                getString(R.string.pref_key_slideshow_duration),
                resources.getInteger(R.integer.pref_key_slideshow_duration_default),
            ) * 1000L
        viewModel.slideshow.observe(this) { newValue ->
            timer?.cancel()
            if (newValue) {
                timer =
                    kotlin.concurrent.timer(
                        name = "imageSlideshow",
                        daemon = true,
                        initialDelay = delay,
                        period = delay,
                    ) {
                        if (!overlayIsVisible) {
                            switchImage(currentPosition + 1, false)
                        }
                    }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        timer?.cancel()
    }

    private fun createDataSupplier(): ImageDataSupplier? {
        val filterArgs = intent.getFilterArgs(INTENT_FILTER_ARGS)
        val galleryId = intent.getStringExtra(INTENT_GALLERY_ID)
        if (filterArgs != null) {
            return DataSupplierFactory(
                StashServer.getCurrentServerVersion(),
            ).create<FindImagesQuery.Data, ImageData, CountImagesQuery.Data>(
                filterArgs,
            ) as ImageDataSupplier
        } else if (galleryId != null && currentPosition >= 0) {
            return ImageDataSupplier(
                DataType.IMAGE.asDefaultFindFilterType,
                ImageFilterType(
                    galleries =
                        Optional.present(
                            MultiCriterionInput(
                                value = Optional.present(listOf(galleryId)),
                                modifier = CriterionModifier.INCLUDES,
                            ),
                        ),
                ),
            )
        } else {
            return null
        }
    }

    private val overlayIsVisible: Boolean
        get() = !overlayFragment.isHidden

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (overlayIsVisible) {
                    supportFragmentManager.commit {
                        setCustomAnimations(
                            androidx.leanback.R.anim.abc_slide_in_bottom,
                            androidx.leanback.R.anim.abc_slide_out_bottom,
                        )
                        hide(overlayFragment)
                    }
                    return true
                } else if (imageViewFragment.isImageZoomedIn()) {
                    imageViewFragment.resetZoom()
                    return true
                }
            } else if (isDpadKey(event.keyCode)) {
                if ((event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER) && !overlayIsVisible) {
                    showOverlay()
                    return true
                } else if (!overlayIsVisible && imageViewFragment.isImageZoomedIn()) {
                    return imageViewFragment.dispatchKeyEvent(event)
                } else if (!overlayIsVisible && !imageViewFragment.isImageZoomedIn()) {
                    // Overlay is not showing and the image is not zoomed in
                    // So maybe move to another image if left or right
                    if (isLeft(event.keyCode)) {
                        switchImage(currentPosition - 1)
                        return true
                    } else if (isRight(event.keyCode)) {
                        switchImage(currentPosition + 1)
                        return true
                    }
                }
            }
            if (event.keyCode != KeyEvent.KEYCODE_BACK && !overlayIsVisible) {
                showOverlay()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showOverlay() {
        supportFragmentManager.commitNow {
            setCustomAnimations(
                androidx.leanback.R.anim.abc_slide_in_bottom,
                androidx.leanback.R.anim.abc_slide_out_bottom,
            )
            show(overlayFragment)
        }
        overlayFragment.requestFocus()
    }

    private fun switchImage(
        newPosition: Int,
        showToast: Boolean = true,
    ) {
        Log.v(TAG, "switchImage $currentPosition => $newPosition")
        if (canScrollImages) {
            if (totalCount != null && newPosition > totalCount!!) {
                if (showToast) {
                    Toast
                        .makeText(this@ImageActivity, "No more images", Toast.LENGTH_SHORT)
                        .show()
                }
            } else if (newPosition >= 0) {
                lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val image = pager.get(newPosition)
                    if (image != null && image.paths.image != null) {
                        currentPosition = newPosition
                        viewModel.setImage(image)
                    } else if (image == null) {
                        if (showToast) {
                            Toast
                                .makeText(this@ImageActivity, "No more images", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            } else if (showToast) {
                Toast.makeText(this, "Already at beginning", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "ImageActivity"
        const val INTENT_IMAGE_ID = "image.id"
        const val INTENT_IMAGE_URL = "image.url"
        const val INTENT_IMAGE_SIZE = "image.size"
        const val INTENT_IMAGE_SLIDESHOW = "image.slideshow"

        const val INTENT_POSITION = "position"
        const val INTENT_GALLERY_ID = "gallery.id"

        const val INTENT_FILTER_ARGS = "filterArgs"

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
