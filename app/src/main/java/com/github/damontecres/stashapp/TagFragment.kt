package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter.PagerEntry
import com.github.damontecres.stashapp.util.getUiTabs

class TagFragment : TabbedFragment(DataType.TAG.name) {
    private lateinit var tagId: String
    private var includeSubTags = false

    override fun getTitleText(): String? = requireActivity().intent.getStringExtra("tagName")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        includeSubTags = requireActivity().intent.getBooleanExtra("includeSubTags", false)
        tagId = requireActivity().intent.getStringExtra("tagId")!!
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.currentServer.observe(viewLifecycleOwner) { server ->
            viewModel.tabs.value =
                listOf(
                    PagerEntry(getString(R.string.stashapp_details)) {
                        TagDetailsFragment()
                    },
                    PagerEntry(DataType.SCENE) {
                        createStashGridFragment(
                            dataType = DataType.SCENE,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_SCENES).findFilter,
                        ) { tags ->
                            SceneFilterType(tags = tags)
                        }
                    },
                    PagerEntry(DataType.GALLERY) {
                        createStashGridFragment(
                            dataType = DataType.GALLERY,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_GALLERIES).findFilter,
                        ) { tags ->
                            GalleryFilterType(tags = tags)
                        }
                    },
                    PagerEntry(DataType.IMAGE) {
                        createStashGridFragment(
                            dataType = DataType.IMAGE,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_IMAGES).findFilter,
                        ) { tags ->
                            ImageFilterType(tags = tags)
                        }.withImageGridClickListener()
                    },
                    PagerEntry(DataType.MARKER) {
                        createStashGridFragment(
                            dataType = DataType.MARKER,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_MARKERS).findFilter,
                        ) { tags ->
                            SceneMarkerFilterType(tags = tags)
                        }
                    },
                    PagerEntry(DataType.PERFORMER) {
                        createStashGridFragment(
                            dataType = DataType.PERFORMER,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_PERFORMERS).findFilter,
                        ) { tags ->
                            PerformerFilterType(tags = tags)
                        }
                    },
                    PagerEntry(DataType.STUDIO) {
                        createStashGridFragment(dataType = DataType.STUDIO, null) { tags ->
                            StudioFilterType(tags = tags)
                        }
                    },
                    PagerEntry(getString(R.string.stashapp_sub_tags)) {
                        createStashGridFragment(dataType = DataType.TAG, null) { tags ->
                            TagFilterType(parents = tags)
                        }
                    },
                    PagerEntry(getString(R.string.stashapp_parent_tags)) {
                        StashGridFragment(
                            dataType = DataType.TAG,
                            objectFilter = TagFilterType(children = createCriterionInput(false)),
                        )
                    },
                ).filter { it.title in getUiTabs(requireContext(), DataType.TAG) }
        }
    }

    private fun createStashGridFragment(
        dataType: DataType,
        defaultFindFilter: StashFindFilter?,
        createObjectFilter: (Optional<HierarchicalMultiCriterionInput>) -> StashDataFilter,
    ): StashGridFragment {
        val fragment =
            StashGridFragment(
                dataType = dataType,
                findFilter = defaultFindFilter,
                objectFilter = createObjectFilter(createCriterionInput(includeSubTags)),
            )
        fragment.subContentSwitchInitialIsChecked = includeSubTags
        fragment.subContentText = getString(R.string.stashapp_include_sub_tag_content)
        fragment.subContentSwitchCheckedListener = { isChecked ->
            val newFilter =
                fragment.filterArgs.copy(
                    objectFilter =
                        createObjectFilter(
                            createCriterionInput(isChecked),
                        ),
                )
            fragment.refresh(newFilter)
        }
        return fragment
    }

    private fun createCriterionInput(subTags: Boolean): Optional.Present<HierarchicalMultiCriterionInput> =
        Optional.present(
            HierarchicalMultiCriterionInput(
                value = Optional.present(listOf(tagId)),
                modifier = CriterionModifier.INCLUDES_ALL,
                depth = Optional.present(if (subTags) -1 else 0),
            ),
        )
}
