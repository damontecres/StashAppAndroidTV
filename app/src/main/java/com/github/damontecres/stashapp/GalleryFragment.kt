package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.leanback.widget.ClassPresenterSelector
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.PerformerInScenePresenter
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.util.name
import com.github.damontecres.stashapp.views.models.GalleryViewModel

class GalleryFragment : TabbedFragment(DataType.GALLERY.name) {
    private val viewModel: GalleryViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        serverViewModel.currentServer.observe(viewLifecycleOwner) {
            viewModel.init(serverViewModel.requireServer(), requireArguments())
        }
        serverViewModel
            .withLiveData(viewModel.item)
            .observe(viewLifecycleOwner) { (server, gallery) ->
                if (gallery == null) {
                    Toast
                        .makeText(
                            requireContext(),
                            "Gallery '${viewModel.itemId}' not found",
                            Toast.LENGTH_LONG,
                        ).show()
                    serverViewModel.navigationManager.goBack()
                    return@observe
                }
                tabViewModel.title.value = gallery.name

                val galleries =
                    Optional.present(
                        MultiCriterionInput(
                            value = Optional.present(listOf(gallery.id)),
                            modifier = CriterionModifier.INCLUDES_ALL,
                        ),
                    )
                if (!tabViewModel.tabs.isInitialized) {
                    tabViewModel.tabs.value =
                        listOf(
                            StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_details)) {
                                GalleryDetailsFragment()
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.IMAGE) {
                                StashGridControlsFragment(
                                    dataType = DataType.IMAGE,
                                    findFilter =
                                        serverViewModel
                                            .requireServer()
                                            .serverPreferences
                                            .getDefaultPageFilter(
                                                PageFilterKey.GALLERY_IMAGES,
                                            ).findFilter,
                                    objectFilter = ImageFilterType(galleries = galleries),
                                )
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.SCENE) {
                                StashGridControlsFragment(
                                    dataType = DataType.SCENE,
                                    objectFilter = SceneFilterType(galleries = galleries),
                                )
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.PERFORMER) {
                                val presenter =
                                    ClassPresenterSelector().addClassPresenter(
                                        PerformerData::class.java,
                                        PerformerInScenePresenter(gallery.date),
                                    )
                                val fragment =
                                    StashGridControlsFragment(
                                        filterArgs =
                                            FilterArgs(
                                                DataType.PERFORMER,
                                                override = DataSupplierOverride.GalleryPerformer(gallery.id),
                                            ),
                                    )
                                fragment.presenterSelector = presenter
                                fragment
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.TAG) {
                                StashGridControlsFragment(
                                    filterArgs =
                                        FilterArgs(
                                            DataType.TAG,
                                            override = DataSupplierOverride.GalleryTag(gallery.id),
                                        ),
                                )
                            },
                        ).filter { it.title in getUiTabs(requireContext(), DataType.GALLERY) }
                }
            }
    }

    companion object {
        private const val TAG = "GalleryFragment"
    }
}
