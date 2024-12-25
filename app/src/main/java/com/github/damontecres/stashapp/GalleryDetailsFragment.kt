package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.StashRatingBar
import com.github.damontecres.stashapp.views.models.GalleryViewModel
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.launch

class GalleryDetailsFragment : Fragment(R.layout.gallery_view) {
    private val serverViewModel: ServerViewModel by activityViewModels()
    private val viewModel: GalleryViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var studioImage: ImageView
    private lateinit var ratingBar: StashRatingBar
    private lateinit var table: TableLayout

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        studioImage = view.findViewById(R.id.studio_image)
        ratingBar = view.findViewById(R.id.gallery_rating_bar)
        table = view.findViewById(R.id.gallery_table)

        viewModel.item.observe(viewLifecycleOwner) { galleryData ->
            if (galleryData == null) {
                return@observe
            }
            addRow(table, R.string.stashapp_details, galleryData.details)
            addRow(table, R.string.stashapp_date, galleryData.date)
            addRow(table, R.string.stashapp_scene_code, galleryData.code)
            addRow(table, R.string.stashapp_photographer, galleryData.photographer)

            if (galleryData.studio?.image_path.isNotNullOrBlank()) {
                StashGlide
                    .with(requireContext(), galleryData.studio!!.image_path!!)
                    .optionalFitCenter()
                    .error(StashPresenter.glideError(requireContext()))
                    .into(studioImage)
            } else {
                studioImage.visibility = View.GONE
            }

            ratingBar.rating100 = galleryData.rating100 ?: 0
            if (readOnlyModeDisabled()) {
                ratingBar.setRatingCallback { rating100 ->
                    val mutationEngine =
                        MutationEngine(StashServer.requireCurrentServer())
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(true)) {
                        val result = mutationEngine.updateGallery(galleryData.id, rating100)
                        if (result != null) {
                            ratingBar.rating100 = result.rating100 ?: 0
                            showSetRatingToast(
                                requireContext(),
                                rating100,
                                ratingBar.ratingAsStars,
                            )
                        }
                    }
                }
            } else {
                ratingBar.disable()
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
                                    value = viewModel.item.value!!.photographer!!,
                                    modifier = CriterionModifier.EQUALS,
                                ),
                            ),
                    )
                val filterArgs =
                    FilterArgs(
                        dataType = DataType.GALLERY,
                        name = viewModel.item.value!!.photographer,
                        objectFilter = objectFilter,
                    )
                serverViewModel.navigationManager.navigate(Destination.Filter(filterArgs))
            }
        }

        table.addView(row)
    }
}
