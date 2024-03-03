package com.github.damontecres.stashapp

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.damontecres.stashapp.util.createGlideUrl

class ImageActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.image_fragment, ImageFragment())
                .commitNow()
        }
    }

    companion object {
        const val TAG = "ImageActivity"
        const val INTENT_IMAGE_ID = "image.id"
        const val INTENT_IMAGE_URL = "image.url"
    }

    class ImageFragment : Fragment(R.layout.image_layout) {
        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            val mainImage = view.findViewById<ImageView>(R.id.image_view_image)
            val imageUrl = requireActivity().intent.getStringExtra(INTENT_IMAGE_URL)!!

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
    }
}
