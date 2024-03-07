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
import com.apollographql.apollo3.api.Optional
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.damontecres.stashapp.api.FindImagesQuery
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.FilterType
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.convertFilter
import com.github.damontecres.stashapp.util.maxFileSize
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class ImageActivity : FragmentActivity() {
    private lateinit var imageFragment: ImageFragment
    private lateinit var dataSupplier: ImageDataSupplier
    private lateinit var pagingSource: StashPagingSource<FindImagesQuery.Data, ImageData>
    private lateinit var currentPageData: CountAndList<ImageData>

    private var currentPosition = -1

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
                .replace(R.id.image_fragment, imageFragment!!)
                .commitNow()

            val pageSize =
                PreferenceManager.getDefaultSharedPreferences(this).getInt("maxSearchResults", 25)

            val galleryId = intent.getStringExtra(INTENT_GALLERY_ID)
            val filterType = FilterType.safeValueOf(intent.getStringExtra(INTENT_FILTER_TYPE))
            currentPosition = intent.getIntExtra(INTENT_POSITION, -1)
            if (galleryId != null && currentPosition >= 0) {
                dataSupplier =
                    ImageDataSupplier(
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
                pagingSource = StashPagingSource(this, pageSize, dataSupplier)
                lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val currentPage = currentPosition / pageSize
                    currentPageData = pagingSource.fetchPage(currentPage, pageSize)
                    canScrollImages = true
                }
            } else if (filterType != null && currentPosition >= 0) {
                Log.v(TAG, "Got a $filterType")
                if (filterType == FilterType.SAVED_FILTER) {
                    val filter = intent.getParcelableExtra<StashSavedFilter>(INTENT_FILTER)!!
                    val queryEngine = QueryEngine(this)
                    lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        val savedFilter = queryEngine.getSavedFilter(filter.savedFilterId)
                        if (savedFilter != null) {
                            val findFilter =
                                queryEngine.updateFilter(
                                    convertFilter(savedFilter.find_filter),
                                    useRandom = true,
                                )?.copy(per_page = Optional.present(pageSize))
                                    ?: DataType.IMAGE.asDefaultFindFilterType
                            val filterParser =
                                FilterParser(ServerPreferences(this@ImageActivity).serverVersion)
                            val imageFilter =
                                filterParser.convertImageObjectFilter(savedFilter.object_filter)
                            dataSupplier =
                                ImageDataSupplier(
                                    findFilter,
                                    imageFilter,
                                )
                            pagingSource =
                                StashPagingSource(this@ImageActivity, pageSize, dataSupplier)
                            lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                                val currentPage = currentPosition / pageSize
                                currentPageData = pagingSource.fetchPage(currentPage, pageSize)
                                canScrollImages = true
                            }
                        } else {
                            Log.w(TAG, "Unknown filter id=${filter.savedFilterId}")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (imageFragment.isOverlayVisible()) {
                    imageFragment.hideOverlay()
                    return true
                }
            } else if (isDpadKey(event.keyCode) && !imageFragment.isOverlayVisible()) {
                if (isLeft(event.keyCode)) {
                    switchImage(currentPosition - 1)
                } else if (isRight(event.keyCode)) {
                    switchImage(currentPosition + 1)
                } else {
                    imageFragment.showOverlay()
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isDpadKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP ||
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            (
                keyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT
            )
    }

    private fun isLeft(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            (
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT
            )
    }

    private fun isRight(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            (
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT
            )
    }

    private fun switchImage(newPosition: Int) {
        Log.v(TAG, "switchImage to $newPosition")
        if (canScrollImages) {
            if (newPosition >= 0) {
                lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val image = fetchImageData(newPosition)
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

    private suspend fun fetchImageData(newPosition: Int): ImageData? {
        val pageSize =
            PreferenceManager.getDefaultSharedPreferences(this).getInt("maxSearchResults", 25)
        val currentPage = newPosition / pageSize
        val listPos = newPosition - currentPage * pageSize
        Log.v(TAG, "fetchImageData currentPage=$currentPage, listPos=$listPos")
        if (listPos < 0) {
            Log.v(TAG, "Fetching previous page")
            // fetch previous page
            currentPageData = pagingSource.fetchPage(currentPage - 1, pageSize)
            val newListPos = newPosition - (currentPage - 1) * pageSize
            return currentPageData.list[newListPos]
        } else if (listPos >= currentPageData.count) {
            // fetch next page
            Log.v(TAG, "Fetching next page")
            currentPageData = pagingSource.fetchPage(currentPage + 1, pageSize)
            val newListPos = newPosition - (currentPage + 1) * pageSize
            if (currentPageData.list.isEmpty() || newListPos < 0) {
                Log.v(TAG, "End of images")
                return null
            }
            return currentPageData.list[newListPos]
        } else {
            Log.v(TAG, "Image already available")
            return currentPageData.list[listPos]
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
    }

    class ImageFragment(val imageId: String, val imageUrl: String, val imageSize: Int = -1) :
        Fragment(R.layout.image_layout) {
        lateinit var titleText: TextView
        lateinit var table: TableLayout
        var image: ImageData? = null

        private var animationDuration by Delegates.notNull<Long>()

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            animationDuration =
                resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            titleText = view.findViewById(R.id.image_view_title)
            val mainImage = view.findViewById<ImageView>(R.id.image_view_image)
            table = view.findViewById(R.id.image_view_table)
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

            StashGlide.with(requireContext(), imageUrl, imageSize)
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

        fun configureUI() {
            val image = image!!
            titleText.text = image.title

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
            listOf(titleText, table).forEach {
                it.alpha = 0.0f
                it.visibility = View.VISIBLE
                it.animate()
                    .alpha(1f)
                    .setDuration(animationDuration)
                    .setListener(null)
            }
        }

        fun hideOverlay() {
            listOf(titleText, table).forEach {
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

            val valueView = row.findViewById<TextView>(R.id.table_row_value)
            valueView.text = value

            table.addView(row)
        }
    }
}
