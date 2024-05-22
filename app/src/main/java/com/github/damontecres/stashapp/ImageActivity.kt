package com.github.damontecres.stashapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.apollographql.apollo3.api.Optional
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.FilterType
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.presenters.PopupOnLongClickListener
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.suppliers.StashSparseFilterFetcher
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.height
import com.github.damontecres.stashapp.util.maxFileSize
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.util.toFindFilterType
import com.github.damontecres.stashapp.util.width
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.StashRatingBar
import com.otaliastudios.zoom.ZoomImageView
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.properties.Delegates

class ImageActivity : FragmentActivity() {
    private lateinit var imageFragment: ImageFragment
    private lateinit var pager: StashSparseFilterFetcher<FindImagesQuery.Data, ImageData>

    private var currentPosition = -1
    private var totalCount: Int? = null

    private var canScrollImages = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        if (savedInstanceState == null) {
            val imageUrl = intent.getStringExtra(INTENT_IMAGE_URL)!!
            val imageId = intent.getStringExtra(INTENT_IMAGE_ID)!!
            val imageSize = intent.getIntExtra(INTENT_IMAGE_SIZE, -1)
            imageFragment = ImageFragment(imageId, imageUrl, imageSize)
            supportFragmentManager.beginTransaction()
                .replace(R.id.image_fragment, imageFragment)
                .commitNow()

            val pageSize =
                PreferenceManager.getDefaultSharedPreferences(this).getInt("maxSearchResults", 25)

            currentPosition = intent.getIntExtra(INTENT_POSITION, -1)
            lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val dataSupplier = createDataSupplier()
                if (dataSupplier != null) {
                    val pagingSource = StashPagingSource(this@ImageActivity, pageSize, dataSupplier)
                    pager = StashSparseFilterFetcher(pagingSource, pageSize)
                    totalCount = pagingSource.getCount()
                    canScrollImages = true
                }
            }
        }
    }

    private suspend fun createDataSupplier(): ImageDataSupplier? {
        val pageSize =
            PreferenceManager.getDefaultSharedPreferences(this).getInt("maxSearchResults", 25)
        val galleryId = intent.getStringExtra(INTENT_GALLERY_ID)
        val filterType = FilterType.safeValueOf(intent.getStringExtra(INTENT_FILTER_TYPE))
        if (galleryId != null && currentPosition >= 0) {
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
        } else if (filterType != null) {
            Log.v(TAG, "Got a $filterType")
            val queryEngine = QueryEngine(this)
            if (filterType == FilterType.SAVED_FILTER) {
                val filter = intent.getParcelableExtra<StashSavedFilter>(INTENT_FILTER)!!
                val savedFilter = queryEngine.getSavedFilter(filter.savedFilterId)
                if (savedFilter != null) {
                    val findFilter =
                        queryEngine.updateFilter(
                            savedFilter.find_filter?.toFindFilterType(),
                            useRandom = true,
                        )?.copy(per_page = Optional.present(pageSize))
                            ?: DataType.IMAGE.asDefaultFindFilterType
                    val filterParser =
                        FilterParser(ServerPreferences(this@ImageActivity).serverVersion)
                    val imageFilter =
                        filterParser.convertImageObjectFilter(savedFilter.object_filter)
                    return ImageDataSupplier(findFilter, imageFilter)
                } else {
                    Log.w(TAG, "Unknown filter id=${filter.savedFilterId}")
                    return null
                }
            } else {
                // CustomFilter
                val filter = intent.getParcelableExtra<StashCustomFilter>(INTENT_FILTER)!!
                val findFilter =
                    queryEngine.updateFilter(
                        filter.asFindFilterType()
                            .copy(per_page = Optional.present(pageSize)),
                    )
                return ImageDataSupplier(findFilter, null)
            }
        } else {
            return null
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (imageFragment.viewCreated && imageFragment.isOverlayVisible()) {
                    imageFragment.hideOverlay()
                    return true
                } else if (imageFragment.viewCreated && imageFragment.isImageZoomedIn()) {
                    imageFragment.resetImageZoom()
                    return true
                }
            } else if (isDpadKey(event.keyCode) && imageFragment.viewCreated) {
                if (!imageFragment.isOverlayVisible() && !imageFragment.isImageZoomedIn()) {
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
                return imageFragment.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun switchImage(newPosition: Int) {
        Log.v(TAG, "switchImage $currentPosition => $newPosition")
        if (canScrollImages) {
            if (totalCount != null && newPosition > totalCount!!) {
                Toast.makeText(this@ImageActivity, "No more images", Toast.LENGTH_SHORT)
                    .show()
            } else if (newPosition >= 0) {
                lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val image = pager.get(newPosition)
                    if (image != null && image.paths.image != null) {
                        currentPosition = newPosition
                        imageFragment =
                            ImageFragment(image.id, image.paths.image, image.maxFileSize)
                        imageFragment.image = image
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.image_fragment, imageFragment)
                            .commitNow()
                    } else if (image == null) {
                        Toast.makeText(this@ImageActivity, "No more images", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(this, "Already at beginning", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "ImageActivity"
        const val INTENT_IMAGE_ID = "image.id"
        const val INTENT_IMAGE_URL = "image.url"
        const val INTENT_IMAGE_SIZE = "image.size"

        const val INTENT_POSITION = "position"
        const val INTENT_GALLERY_ID = "gallery.id"
        const val INTENT_FILTER = "filter"
        const val INTENT_FILTER_TYPE = "filter.type"

        fun isDpadKey(keyCode: Int): Boolean {
            return isDirectionalDpadKey(keyCode) ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER
        }

        fun isDirectionalDpadKey(keyCode: Int): Boolean {
            return keyCode == KeyEvent.KEYCODE_DPAD_UP ||
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
        }

        fun isLeft(keyCode: Int): Boolean {
            return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT
                )
        }

        fun isRight(keyCode: Int): Boolean {
            return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT
                )
        }

        fun isUp(keyCode: Int): Boolean {
            return keyCode == KeyEvent.KEYCODE_DPAD_UP || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (
                    keyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT
                )
        }

        fun isDown(keyCode: Int): Boolean {
            return keyCode == KeyEvent.KEYCODE_DPAD_DOWN || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT ||
                        keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT
                )
        }
    }

    class ImageFragment(
        val imageId: String,
        val imageUrl: String,
        val imageSize: Int = -1,
    ) :
        Fragment(R.layout.image_layout) {
        lateinit var mainImage: ZoomImageView
        lateinit var bottomOverlay: View
        lateinit var titleText: TextView
        lateinit var oCounterTextView: TextView
        lateinit var table: TableLayout
        lateinit var ratingBar: StashRatingBar
        var image: ImageData? = null

        private var animationDuration by Delegates.notNull<Long>()

        var viewCreated = false
        private val duration = 200L
        private var duringAnimation = false

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            bottomOverlay = view.findViewById(R.id.image_bottom_overlay)
            animationDuration =
                resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            titleText = view.findViewById(R.id.image_view_title)
            mainImage = view.findViewById(R.id.image_view_image)
            table = view.findViewById(R.id.image_view_table)
            oCounterTextView = view.findViewById(R.id.o_counter_text)
            ratingBar = view.findViewById(R.id.rating_bar)

            Log.v(TAG, "imageId=$imageId")
            if (image != null) {
                configureUI()
            } else {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val queryEngine = QueryEngine(requireContext())
                    image = queryEngine.getImage(imageId)!!
                    configureUI()
                }
            }

            ratingBar.nextFocusDownId = R.id.o_counter_button
            val focusListener = ImageButtonFocusListener(ratingBar)

            val rotateRightButton = view.findViewById<Button>(R.id.rotate_right_button)
            rotateRightButton.onFocusChangeListener = focusListener
            rotateRightButton.setOnClickListener(RotateImageListener(90f))
            rotateRightButton.nextFocusUpId = ratingBar.focusableViewId

            val rotateLeftButton = view.findViewById<Button>(R.id.rotate_left_button)
            rotateLeftButton.onFocusChangeListener = focusListener
            rotateLeftButton.setOnClickListener(RotateImageListener(-90f))
            rotateLeftButton.nextFocusUpId = ratingBar.focusableViewId

            val flipButton = view.findViewById<Button>(R.id.flip_button)
            flipButton.onFocusChangeListener = focusListener
            flipButton.setOnClickListener {
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
            flipButton.nextFocusUpId = ratingBar.focusableViewId

            val resetButton = view.findViewById<Button>(R.id.reset_button)
            resetButton.onFocusChangeListener = focusListener
            resetButton.setOnClickListener {
                resetImageZoom()
                if (!duringAnimation) {
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
            resetButton.nextFocusUpId = ratingBar.focusableViewId

            val mutationEngine = MutationEngine(requireContext())
            val oCounterButton = view.findViewById<ImageButton>(R.id.o_counter_button)
            oCounterButton.onFocusChangeListener = focusListener
            oCounterButton.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val newOCounter = mutationEngine.incrementImageOCounter(imageId)
                    oCounterTextView.text = newOCounter.count.toString()
                }
            }
            oCounterButton.setOnLongClickListener(
                PopupOnLongClickListener(
                    listOf(
                        "Increment",
                        "Decrement",
                        "Reset",
                    ),
                    225,
                ) { parent, view, position, id ->
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        val newOCounter =
                            when (position) {
                                0 -> mutationEngine.incrementImageOCounter(imageId)
                                1 -> mutationEngine.decrementImageOCounter(imageId)
                                2 -> mutationEngine.resetImageOCounter(imageId)
                                else -> null
                            }
                        oCounterTextView.text = newOCounter?.count?.toString()
                    }
                },
            )
            oCounterButton.nextFocusUpId = ratingBar.focusableViewId

            val zoomInButton = view.findViewById<Button>(R.id.zoom_in_button)
            zoomInButton.onFocusChangeListener = focusListener
            zoomInButton.setOnClickListener {
                mainImage.zoomIn()
            }
            zoomInButton.nextFocusUpId = ratingBar.focusableViewId

            val zoomOutButton = view.findViewById<Button>(R.id.zoom_out_button)
            zoomOutButton.onFocusChangeListener = focusListener
            zoomOutButton.setOnClickListener {
                mainImage.zoomOut()
            }
            zoomOutButton.nextFocusUpId = ratingBar.focusableViewId

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

            StashGlide.with(requireContext(), imageUrl, imageSize)
                .placeholder(placeholder)
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
            viewCreated = true
        }

        fun configureUI() {
            val image = image!!
            titleText.text = image.title

            ratingBar.rating100 = image.rating100 ?: 0
            ratingBar.setRatingCallback {
                val mutationEngine = MutationEngine(requireContext(), true)
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val result = mutationEngine.updateImage(image.id, rating100 = it)
                    ratingBar.rating100 = result?.rating100 ?: 0
                    showSetRatingToast(requireContext(), it, ratingBar.ratingAsStars)
                }
            }

            oCounterTextView.text = image.o_counter.toString()

            if (image.visual_files.isNotEmpty()) {
                val imageFile = image.visual_files.first()
                val imageHeight = imageFile.height
                val imageWidth = imageFile.width
                if (imageHeight != null && imageWidth != null) {
                    addRow(R.string.stashapp_dimensions, "${imageWidth}x$imageHeight")
                }
            }
            addRow(R.string.stashapp_studio, image.studio?.studioData?.name)
            addRow(R.string.stashapp_date, image.date)
            addRow(R.string.stashapp_photographer, image.photographer)
            addRow(R.string.stashapp_details, image.details)
            addRow(
                R.string.stashapp_tags,
                concatIfNotBlank(", ", image.tags.map { it.tagData.name }),
            )
            addRow(
                R.string.stashapp_performers,
                concatIfNotBlank(", ", image.performers.map { it.performerData.name }),
            )
        }

        fun isOverlayVisible(): Boolean {
            return titleText.isVisible
        }

        fun showOverlay() {
            bottomOverlay.requestFocus()
            listOf(titleText, bottomOverlay).forEach {
                it.alpha = 0.0f
                it.visibility = View.VISIBLE
                it.animate()
                    .alpha(1f)
                    .setDuration(animationDuration)
                    .setListener(null)
            }
        }

        fun hideOverlay() {
            listOf(titleText, bottomOverlay).forEach {
                it.alpha = 1.0f
                it.visibility = View.VISIBLE
                it.animate()
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                it.visibility = View.GONE
                            }
                        },
                    )
            }
        }

        private fun addRow(
            key: Int,
            value: String?,
        ) {
            if (value.isNullOrBlank()) {
                return
            }
            val keyString = getString(key) + ":"

            val row =
                requireActivity().layoutInflater.inflate(
                    R.layout.table_row,
                    table,
                    false,
                ) as TableRow

            val keyView = row.findViewById<TextView>(R.id.table_row_key)
            keyView.text = keyString
            keyView.textSize = 16f

            val valueView = row.findViewById<TextView>(R.id.table_row_value)
            valueView.text = value
            valueView.textSize = 16f

            table.addView(row)
        }

        private class ImageButtonFocusListener(val ratingBar: StashRatingBar) :
            StashOnFocusChangeListener(ratingBar.context) {
            override fun onFocusChange(
                v: View,
                hasFocus: Boolean,
            ) {
                super.onFocusChange(v, hasFocus)
                if (hasFocus) {
                    ratingBar.nextFocusDownId = v.id
                }
            }
        }

        private inner class RotateImageListener(val rotation: Float) : View.OnClickListener {
            override fun onClick(v: View?) {
                if (!duringAnimation) {
                    duringAnimation = true

                    val scale = if (isImageRotated()) 1f else calculateRotationScale(mainImage)
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
        }

        private fun isImageRotated(): Boolean {
            val rotation = abs(mainImage.rotation)
            return rotation == 90f || rotation == 270f
        }

        private fun calculateRotationScale(mainImage: ImageView): Float {
            val viewHeight = mainImage.height.toFloat()
            val viewWidth = mainImage.width.toFloat()
            val imageHeight = image!!.visual_files.first().height!!.toFloat()
            val imageWidth = image!!.visual_files.first().width!!.toFloat()
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

        fun isImageZoomedIn(): Boolean {
            return (mainImage.zoom * 100).toInt() > 100
        }

        fun resetImageZoom() {
            mainImage.zoomTo(1.0f, true)
        }

        fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (isOverlayVisible()) {
                return false
            }
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
                } else {
                    showOverlay()
                }
            } else {
                showOverlay()
            }
            return true
        }
    }
}
