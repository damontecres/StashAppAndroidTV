package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.views.models.StudioViewModel

class StudioFragment : TabbedFragment(DataType.STUDIO.name) {
    private val viewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(requireArguments())
    }

    override fun getTitleText(): String? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.item.observe(viewLifecycleOwner) { studio ->
            if (studio == null) {
                Toast.makeText(requireContext(), "Studio not found", Toast.LENGTH_LONG).show()
                serverViewModel.navigationManager.goBack()
                return@observe
            }
            tabViewModel.title.value = studio.name
            val server = serverViewModel.requireServer()
            tabViewModel.tabs.value =
                listOf(
                    StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_details)) {
                        StudioDetailsFragment()
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.SCENE) {
                        createStashGridFragment(
                            studio.id,
                            DataType.SCENE,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.STUDIO_SCENES).findFilter,
                        ) { studios ->
                            SceneFilterType(studios = studios)
                        }
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.GALLERY) {
                        createStashGridFragment(
                            studio.id,
                            DataType.GALLERY,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.STUDIO_GALLERIES).findFilter,
                        ) { studios ->
                            GalleryFilterType(studios = studios)
                        }
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.IMAGE) {
                        createStashGridFragment(
                            studio.id,
                            DataType.IMAGE,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.STUDIO_IMAGES).findFilter,
                        ) { studios ->
                            ImageFilterType(studios = studios)
                        }
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.PERFORMER) {
                        createStashGridFragment(
                            studio.id,
                            DataType.PERFORMER,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.STUDIO_PERFORMERS).findFilter,
                        ) { studios ->
                            PerformerFilterType(studios = studios)
                        }
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.GROUP) {
                        createStashGridFragment(
                            studio.id,
                            DataType.GROUP,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.STUDIO_GROUPS).findFilter,
                        ) { studios ->
                            GroupFilterType(studios = studios)
                        }
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.TAG) {
                        StashGridFragment(
                            FilterArgs(
                                DataType.TAG,
                                override = DataSupplierOverride.StudioTags(studio.id),
                            ),
                        )
                    },
                    StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_subsidiary_studios)) {
                        StashGridFragment(
                            dataType = DataType.STUDIO,
                            findFilter = server.serverPreferences.getDefaultFilter(PageFilterKey.STUDIO_CHILDREN).findFilter,
                            objectFilter =
                                StudioFilterType(
                                    parents =
                                        Optional.present(
                                            MultiCriterionInput(
                                                value = Optional.present(listOf(studio.id)),
                                                modifier = CriterionModifier.INCLUDES,
                                            ),
                                        ),
                                ),
                        )
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.MARKER) {
                        createStashGridFragment(studio.id, DataType.MARKER, null) { studios ->
                            SceneMarkerFilterType(
                                scene_filter =
                                    Optional.present(
                                        SceneFilterType(studios = studios),
                                    ),
                            )
                        }
                    },
                ).filter { it.title in getUiTabs(requireContext(), DataType.STUDIO) }
        }
    }

    private fun createStashGridFragment(
        studioId: String,
        dataType: DataType,
        defaultFindFilter: StashFindFilter?,
        createObjectFilter: (Optional<HierarchicalMultiCriterionInput>) -> StashDataFilter,
    ): StashGridFragment {
        val fragment =
            StashGridFragment(
                dataType = dataType,
                findFilter = defaultFindFilter,
                objectFilter = createObjectFilter(createCriterionInput(false, studioId)),
            )
        fragment.subContentSwitchInitialIsChecked = false
        fragment.subContentText = getString(R.string.stashapp_include_sub_studio_content)
        fragment.subContentSwitchCheckedListener = { isChecked ->
            val newFilter =
                fragment.filterArgs.copy(
                    objectFilter =
                        createObjectFilter(
                            createCriterionInput(isChecked, studioId),
                        ),
                )
            fragment.refresh(newFilter)
        }
        return fragment
    }

    private fun createCriterionInput(
        includeSub: Boolean,
        studioId: String,
    ): Optional.Present<HierarchicalMultiCriterionInput> =
        Optional.present(
            HierarchicalMultiCriterionInput(
                value = Optional.present(listOf(studioId)),
                modifier = CriterionModifier.INCLUDES,
                depth = Optional.present(if (includeSub) -1 else 0),
            ),
        )
}
