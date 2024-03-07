package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.itemclick.ImageGridClickedListener
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.util.ImageComparator

class GalleryActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.grid_view)
        if (savedInstanceState == null) {
            val galleryId = intent.getStringExtra(INTENT_GALLERY_ID)!!
            val galleryName = intent.getStringExtra(INTENT_GALLERY_NAME)
            findViewById<TextView>(R.id.grid_title).text = galleryName

            val fragment =
                StashGridFragment(
                    ImageComparator,
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
                    ),
                )
            fragment.onItemViewClickedListener =
                ImageGridClickedListener(this, fragment) {
                    it.putExtra(ImageActivity.INTENT_GALLERY_ID, galleryId)
                }

            supportFragmentManager.beginTransaction().replace(R.id.grid_fragment, fragment)
                .commitNow()
        }
    }

    companion object {
        const val INTENT_GALLERY_ID = "gallery.id"
        const val INTENT_GALLERY_NAME = "gallery.name"
    }
}
