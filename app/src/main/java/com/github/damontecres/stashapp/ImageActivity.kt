package com.github.damontecres.stashapp

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
//            val lp = mainImage.layoutParams
//            lp.width=view.width
//            lp.height=view.height
//            mainImage.layoutParams=lp

            val imageUrl = requireActivity().intent.getStringExtra(INTENT_IMAGE_URL)!!
            Glide.with(requireContext())
                .load(createGlideUrl(imageUrl, requireContext()))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .listener(
                    object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean,
                        ): Boolean {
                            Log.v(TAG, "onLoadFailed")
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable?>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean,
                        ): Boolean {
                            Log.v(TAG, "onResourceReady ${resource.bounds}")
                            return false
                        }
                    },
                )
                .into(mainImage)
        }
    }
}
