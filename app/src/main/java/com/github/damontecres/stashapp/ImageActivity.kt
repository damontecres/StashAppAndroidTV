package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.createGlideUrl
import kotlinx.coroutines.launch

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
                if (imageFragment.titleText.isVisible) {
                    imageFragment.hideOverlay()
                    return true
                }
            } else if (!imageFragment.titleText.isVisible) {
                imageFragment.showOverlay()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        const val TAG = "ImageActivity"
        const val INTENT_IMAGE_ID = "image.id"
        const val INTENT_IMAGE_URL = "image.url"
    }

    class ImageFragment : Fragment(R.layout.image_layout) {
        lateinit var titleText: TextView
        lateinit var table: TableLayout
        lateinit var image: ImageData

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            titleText = view.findViewById(R.id.image_view_title)
            val mainImage = view.findViewById<ImageView>(R.id.image_view_image)
            table = view.findViewById(R.id.image_view_table)
            val imageUrl = requireActivity().intent.getStringExtra(INTENT_IMAGE_URL)!!
            val imageId = requireActivity().intent.getStringExtra(INTENT_IMAGE_ID)!!
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

            // Images larger than 5mb need disk cache
            // https://github.com/bumptech/glide/issues/4950
            Glide.with(requireContext())
                .load(createGlideUrl(imageUrl, requireContext()))
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

        fun showOverlay() {
            titleText.visibility = View.VISIBLE
            table.visibility = View.VISIBLE
        }

        fun hideOverlay() {
            titleText.visibility = View.GONE
            table.visibility = View.GONE
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
