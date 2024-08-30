package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.leanback.widget.ClassPresenterSelector
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Gallery
import com.github.damontecres.stashapp.presenters.PerformerInScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.StashRatingBar
import kotlinx.coroutines.launch

class GalleryActivity : TabbedGridFragmentActivity(R.layout.gallery_activity) {
    private lateinit var gallery: Gallery

    override fun onCreate(savedInstanceState: Bundle?) {
        gallery = intent.getParcelableExtra(INTENT_GALLERY_OBJ)!!
        super.onCreate(savedInstanceState)
        viewModel.title.value = gallery.name
    }

    override fun getPagerAdapter(): StashFragmentPagerAdapter {
        return PagerAdapter(gallery, supportFragmentManager)
    }

    private class PagerAdapter(
        private val gallery: Gallery,
        fm: FragmentManager,
    ) :
        StashFragmentPagerAdapter(
                listOf(
                    PagerEntry("Details", null),
                    PagerEntry(DataType.IMAGE),
                    PagerEntry(DataType.SCENE),
                    PagerEntry(DataType.PERFORMER),
                    PagerEntry(DataType.TAG),
                ),
                fm,
            ) {
        override fun getFragment(position: Int): Fragment {
            val galleries =
                Optional.present(
                    MultiCriterionInput(
                        value = Optional.present(listOf(gallery.id)),
                        modifier = CriterionModifier.INCLUDES_ALL,
                    ),
                )

            val fragment =
                when (position) {
                    0 -> {
                        GalleryFragment(gallery)
                    }

                    1 -> {
                        StashGridFragment(
                            dataType = DataType.IMAGE,
                            objectFilter = ImageFilterType(galleries = galleries),
                        ).withImageGridClickListener()
                    }

                    2 ->
                        StashGridFragment(
                            dataType = DataType.SCENE,
                            objectFilter = SceneFilterType(galleries = galleries),
                        )

                    3 -> {
                        val presenter =
                            ClassPresenterSelector().addClassPresenter(
                                PerformerData::class.java,
                                PerformerInScenePresenter(gallery.date),
                            )
                        val fragment =
                            StashGridFragment(
                                filterArgs =
                                    FilterArgs(
                                        DataType.PERFORMER,
                                        override = DataSupplierOverride.GalleryPerformer(gallery.id),
                                    ),
                            )
                        fragment.presenterSelector = presenter
                        fragment
                    }

                    4 ->
                        StashGridFragment(
                            filterArgs =
                                FilterArgs(
                                    DataType.TAG,
                                    override = DataSupplierOverride.GalleryTag(gallery.id),
                                ),
                        )

                    else -> throw IllegalArgumentException()
                }
            return fragment
        }
    }

    class GalleryFragment() : Fragment(R.layout.gallery_view) {
        private lateinit var gallery: Gallery
        private lateinit var galleryData: GalleryData

        private lateinit var studioImage: ImageView
        private lateinit var ratingBar: StashRatingBar
        private lateinit var table: TableLayout

        constructor(gallery: Gallery) : this() {
            this.gallery = gallery
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)

            if (savedInstanceState != null) {
                gallery = savedInstanceState.getParcelable("gallery")!!
            }

            studioImage = view.findViewById(R.id.studio_image)
            ratingBar = view.findViewById(R.id.gallery_rating_bar)
            table = view.findViewById(R.id.gallery_table)

            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(true)) {
                val queryEngine = QueryEngine(requireContext())
                galleryData = queryEngine.getGalleries(listOf(gallery.id)).first()

                addRow(table, R.string.stashapp_details, galleryData.details)
                addRow(table, R.string.stashapp_date, galleryData.date)
                addRow(table, R.string.stashapp_scene_code, galleryData.code)
                addRow(table, R.string.stashapp_photographer, galleryData.photographer)

                if (galleryData.studio?.image_path.isNotNullOrBlank()) {
                    StashGlide.with(requireContext(), galleryData.studio!!.image_path!!)
                        .optionalFitCenter()
                        .error(StashPresenter.glideError(requireContext()))
                        .into(studioImage)
                } else {
                    studioImage.visibility = View.GONE
                }

                ratingBar.rating100 = galleryData.rating100 ?: 0
                ratingBar.setRatingCallback { rating100 ->
                    val mutationEngine = MutationEngine(requireContext(), true)
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(true)) {
                        val result = mutationEngine.updateGallery(galleryData.id, rating100)
                        if (result != null) {
                            ratingBar.rating100 = result.rating100 ?: 0
                            showSetRatingToast(requireContext(), rating100, ratingBar.ratingAsStars)
                        }
                    }
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

            val layoutId =
                if (key == R.string.stashapp_photographer) {
                    R.layout.table_row_photographer
                } else {
                    R.layout.table_row
                }

            val row =
                requireActivity().layoutInflater.inflate(
                    layoutId,
                    table,
                    false,
                ) as TableRow

            val keyView = row.findViewById<TextView>(R.id.table_row_key)
            keyView.text = keyString
            keyView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.table_text_size_large),
            )

            val valueView = row.findViewById<TextView>(R.id.table_row_value)
            valueView.text = value
            valueView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.table_text_size_large),
            )
            if (key == R.string.stashapp_photographer) {
                valueView.onFocusChangeListener = StashOnFocusChangeListener(requireContext())
                valueView.setOnClickListener {
                    val objectFilter =
                        GalleryFilterType(
                            photographer =
                                Optional.present(
                                    StringCriterionInput(
                                        value = galleryData.photographer!!,
                                        modifier = CriterionModifier.EQUALS,
                                    ),
                                ),
                        )
                    val filterArgs =
                        FilterArgs(
                            dataType = DataType.GALLERY,
                            name = galleryData.photographer,
                            objectFilter = objectFilter,
                        )
                    val intent =
                        Intent(requireContext(), FilterListActivity::class.java)
                            .putExtra(FilterListActivity.INTENT_FILTER_ARGS, filterArgs)
                    requireContext().startActivity(intent)
                }
            }

            table.addView(row)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("gallery", gallery)
    }

    companion object {
        const val INTENT_GALLERY_OBJ = "gallery"
    }
}
