package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
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
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.grid_fragment,
                    StashGridFragment(
                        ImageComparator,
                        ImageDataSupplier(
                            FindFilterType(
                                sort = Optional.present("path"),
                                direction = Optional.present(SortDirectionEnum.ASC),
                            ),
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
                    ),
                ).commitNow()
        }
    }

    companion object {
        const val INTENT_GALLERY_ID = "gallery.id"
        const val INTENT_GALLERY_NAME = "gallery.name"
    }
}
