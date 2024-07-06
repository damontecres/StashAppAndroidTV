package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.bumptech.glide.request.target.Target
import com.github.damontecres.stashapp.api.FindGalleryQuery
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.ImageComparator
import com.github.damontecres.stashapp.util.ListFragmentPagerAdapter
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.TagComparator
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.name
import com.github.damontecres.stashapp.util.onlyScrollIfNeeded
import kotlinx.coroutines.launch

class GalleryActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        if (savedInstanceState == null) {
            val galleryId = intent.getStringExtra(INTENT_GALLERY_ID)!!
            val galleryName = intent.getStringExtra(INTENT_GALLERY_NAME)

            val cardSize =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt("cardSize", getString(R.string.card_size_default))
            // At medium size, 3 scenes fit in the space vs 5 normally
            val columns = cardSize * 3.0 / 5

            val tabLayout = findViewById<LeanbackTabLayout>(R.id.gallery_tab_layout)
            val viewPager = findViewById<LeanbackViewPager>(R.id.gallery_view_pager)

            val queryEngine = QueryEngine(this)
            lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val gallery = queryEngine.getGalleries(listOf(galleryId)).first()
                viewPager.adapter = PagerAdapter(gallery, columns, supportFragmentManager)
                tabLayout.setupWithViewPager(viewPager)

                tabLayout.nextFocusDownId = R.id.gallery_view_pager
                tabLayout.children.forEach { it.nextFocusDownId = R.id.gallery_view_pager }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.gallery_details, GalleryFragment(gallery))
                    .commitNow()
            }
        }
    }

    inner class PagerAdapter(
        private val gallery: GalleryData,
        private val columns: Double,
        fm: FragmentManager,
    ) :
        ListFragmentPagerAdapter(
                listOf(
                    this.getString(DataType.IMAGE.pluralStringId),
                    this.getString(DataType.SCENE.pluralStringId),
                    this.getString(DataType.PERFORMER.pluralStringId),
                    this.getString(DataType.TAG.pluralStringId),
                ),
                fm,
            ) {
        private fun getColumns(dataType: DataType): Int {
            return (columns * dataType.defaultCardRatio).toInt()
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 ->
                    StashGridFragment(
                        ImageComparator,
                        ImageDataSupplier(
                            DataType.IMAGE.asDefaultFindFilterType,
                            ImageFilterType(
                                galleries =
                                    Optional.present(
                                        MultiCriterionInput(
                                            value = Optional.present(listOf(gallery.id)),
                                            modifier = CriterionModifier.INCLUDES_ALL,
                                        ),
                                    ),
                            ),
                        ),
                        getColumns(DataType.IMAGE),
                    )

                1 ->
                    StashGridFragment(
                        SceneComparator,
                        SceneDataSupplier(
                            DataType.SCENE.asDefaultFindFilterType,
                            SceneFilterType(
                                galleries =
                                    Optional.present(
                                        MultiCriterionInput(
                                            value = Optional.present(listOf(gallery.id)),
                                            modifier = CriterionModifier.INCLUDES_ALL,
                                        ),
                                    ),
                            ),
                        ),
                        getColumns(DataType.SCENE),
                    )

                2 ->
                    StashGridFragment(
                        PerformerComparator,
                        GalleryPerformerDataSupplier(gallery),
                        getColumns(DataType.PERFORMER),
                    )

                3 ->
                    StashGridFragment(
                        TagComparator,
                        GalleryTagDataSupplier(gallery),
                        getColumns(DataType.TAG),
                    )

                else -> throw IllegalArgumentException()
            }
        }
    }

    class GalleryFragment(val gallery: GalleryData) : Fragment(R.layout.gallery_view) {
        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            val gallerySidebar = view.findViewById<View>(R.id.gallery_sidebar)
            val titleTextView = view.findViewById<TextView>(R.id.gallery_name)
            val studioImage = view.findViewById<ImageView>(R.id.studio_image)
            val descriptionTextView = view.findViewById<TextView>(R.id.gallery_description)
            val table = view.findViewById<TableLayout>(R.id.gallery_table)

            titleTextView.text = gallery.name
            descriptionTextView.text = gallery.details

            addRow(table, R.string.stashapp_date, gallery.date)
            addRow(table, R.string.stashapp_scene_code, gallery.code)
            addRow(table, R.string.stashapp_photographer, gallery.photographer)

            view.findViewById<ScrollView>(R.id.gallery_scrollview).onlyScrollIfNeeded()

            if (gallery.studio?.image_path.isNotNullOrBlank()) {
                StashGlide.with(requireContext(), gallery.studio!!.image_path!!)
                    .override(gallerySidebar.width, Target.SIZE_ORIGINAL)
                    .centerCrop()
                    .error(StashPresenter.glideError(requireContext()))
                    .into(studioImage)
            } else {
                studioImage.visibility = View.GONE
            }
        }

        private fun addRow(
            table: TableLayout,
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

    private class GalleryPerformerDataSupplier(private val gallery: GalleryData) :
        StashPagingSource.DataSupplier<FindGalleryQuery.Data, PerformerData, FindGalleryQuery.Data> {
        override val dataType: DataType
            get() = DataType.PERFORMER

        override fun createQuery(filter: FindFilterType?): Query<FindGalleryQuery.Data> {
            return FindGalleryQuery(gallery.id)
        }

        override fun getDefaultFilter(): FindFilterType {
            return DataType.PERFORMER.asDefaultFindFilterType
        }

        override fun createCountQuery(filter: FindFilterType?): Query<FindGalleryQuery.Data> {
            return FindGalleryQuery(gallery.id)
        }

        override fun parseCountQuery(data: FindGalleryQuery.Data): Int {
            return data.findGallery?.galleryData?.performers?.size ?: 0
        }

        override fun parseQuery(data: FindGalleryQuery.Data): List<PerformerData> {
            return data.findGallery?.galleryData?.performers?.map { it.performerData }.orEmpty()
        }
    }

    private class GalleryTagDataSupplier(private val gallery: GalleryData) :
        StashPagingSource.DataSupplier<FindGalleryQuery.Data, TagData, FindGalleryQuery.Data> {
        override val dataType: DataType
            get() = DataType.TAG

        override fun createQuery(filter: FindFilterType?): Query<FindGalleryQuery.Data> {
            return FindGalleryQuery(gallery.id)
        }

        override fun getDefaultFilter(): FindFilterType {
            return DataType.TAG.asDefaultFindFilterType
        }

        override fun createCountQuery(filter: FindFilterType?): Query<FindGalleryQuery.Data> {
            return FindGalleryQuery(gallery.id)
        }

        override fun parseCountQuery(data: FindGalleryQuery.Data): Int {
            return data.findGallery?.galleryData?.performers?.size ?: 0
        }

        override fun parseQuery(data: FindGalleryQuery.Data): List<TagData> {
            return data.findGallery?.galleryData?.tags?.map { it.tagData }.orEmpty()
        }
    }

    companion object {
        const val INTENT_GALLERY_ID = "gallery.id"
        const val INTENT_GALLERY_NAME = "gallery.name"
    }
}
