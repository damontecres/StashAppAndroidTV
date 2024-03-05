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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.concatIfNotBlank
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class ImageActivity : FragmentActivity() {
    private val imageFragment = ImageFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.image_fragment, imageFragment)
                .commitNow()
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
                imageFragment.showOverlay()
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

    companion object {
        const val TAG = "ImageActivity"
        const val INTENT_IMAGE_ID = "image.id"
        const val INTENT_IMAGE_URL = "image.url"
        const val INTENT_IMAGE_SIZE = "image.size"
    }

    class ImageFragment : Fragment(R.layout.image_layout) {
        lateinit var titleText: TextView
        lateinit var table: TableLayout
        lateinit var image: ImageData

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
            val imageUrl = requireActivity().intent.getStringExtra(INTENT_IMAGE_URL)!!
            val imageId = requireActivity().intent.getStringExtra(INTENT_IMAGE_ID)!!
            val imageSize = requireActivity().intent.getIntExtra(INTENT_IMAGE_SIZE, -1)
            Log.v(TAG, "imageId=$imageId")
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val queryEngine = QueryEngine(requireContext())
                image = queryEngine.getImage(imageId)!!
                Log.v(TAG, "image.id=${image.id}")
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
