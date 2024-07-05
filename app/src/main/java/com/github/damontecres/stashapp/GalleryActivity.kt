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
import com.bumptech.glide.request.target.Target
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
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
            viewPager.adapter = PagerAdapter(galleryId, columns, supportFragmentManager)
            tabLayout.setupWithViewPager(viewPager)

            tabLayout.nextFocusDownId = R.id.gallery_view_pager
            tabLayout.children.forEach { it.nextFocusDownId = R.id.gallery_view_pager }

            supportFragmentManager.beginTransaction()
                .replace(R.id.gallery_details, GalleryFragment(galleryId))
                .commitNow()
        }
    }

    inner class PagerAdapter(
        private val galleryId: String,
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
                                            value = Optional.present(listOf(galleryId)),
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
                                            value = Optional.present(listOf(galleryId)),
                                            modifier = CriterionModifier.INCLUDES_ALL,
                                        ),
                                    ),
                            ),
                        ),
                        getColumns(DataType.SCENE),
                    )

                2 ->
                    // TODO
                    StashGridFragment(
                        PerformerComparator,
                        PerformerDataSupplier(
                            DataType.PERFORMER.asDefaultFindFilterType,
                            PerformerFilterType(
//                            galleries =
//                            Optional.present(
//                                MultiCriterionInput(
//                                    value = Optional.present(listOf(galleryId)),
//                                    modifier = CriterionModifier.INCLUDES_ALL,
//                                ),
//                            ),
                            ),
                        ),
                        getColumns(DataType.PERFORMER),
                    )

                3 ->
                    // TODO
                    StashGridFragment(
                        TagComparator,
                        TagDataSupplier(
                            DataType.TAG.asDefaultFindFilterType,
                            TagFilterType(
//                            galleries =
//                            Optional.present(
//                                MultiCriterionInput(
//                                    value = Optional.present(listOf(galleryId)),
//                                    modifier = CriterionModifier.INCLUDES_ALL,
//                                ),
//                            ),
                            ),
                        ),
                        getColumns(DataType.PERFORMER),
                    )

                else -> throw IllegalArgumentException()
            }
        }
    }

    class GalleryFragment(val galleryId: String) : Fragment(R.layout.gallery_view) {
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

            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val queryEngine = QueryEngine(requireContext())
                val gallery = queryEngine.getGalleries(listOf(galleryId)).first()

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

    companion object {
        const val INTENT_GALLERY_ID = "gallery.id"
        const val INTENT_GALLERY_NAME = "gallery.name"
    }
}
