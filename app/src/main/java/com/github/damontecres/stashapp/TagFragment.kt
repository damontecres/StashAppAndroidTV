package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
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
import com.github.damontecres.stashapp.views.models.StashGridViewModel
import com.github.damontecres.stashapp.views.models.TagViewModel

class TagFragment : TabbedFragment(DataType.TAG.name) {
    private val viewModel: TagViewModel by viewModels()
    private val stashGridViewModel: StashGridViewModel by viewModels()

    private var includeSubTags = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(requireArguments())
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.item.observe(viewLifecycleOwner) { tag ->
            if (tag == null) {
                Toast.makeText(requireContext(), "Tag not found", Toast.LENGTH_LONG).show()
                serverViewModel.navigationManager.goBack()
                return@observe
            }
            tabViewModel.title.value = tag.name

            val server = serverViewModel.requireServer()
            tabViewModel.tabs.value =
                listOf(
                    PagerEntry(getString(R.string.stashapp_details)) {
                        TagDetailsFragment()
                    },
                    PagerEntry(DataType.SCENE) {
                        createStashGridFragment(
                            tag.id,
                            dataType = DataType.SCENE,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_SCENES).findFilter,
                        ) { tags ->
                            SceneFilterType(tags = tags)
                        }
                    },
                    PagerEntry(DataType.GALLERY) {
                        createStashGridFragment(
                            tag.id,
                            dataType = DataType.GALLERY,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_GALLERIES).findFilter,
                        ) { tags ->
                            GalleryFilterType(tags = tags)
                        }
                    },
                    PagerEntry(DataType.IMAGE) {
                        createStashGridFragment(
                            tag.id,
                            dataType = DataType.IMAGE,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_IMAGES).findFilter,
                        ) { tags ->
                            ImageFilterType(tags = tags)
                        }
                    },
                    PagerEntry(DataType.MARKER) {
                        createStashGridFragment(
                            tag.id,
                            dataType = DataType.MARKER,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_MARKERS).findFilter,
                        ) { tags ->
                            SceneMarkerFilterType(tags = tags)
                        }
                    },
                    PagerEntry(DataType.PERFORMER) {
                        createStashGridFragment(
                            tag.id,
                            dataType = DataType.PERFORMER,
                            server.serverPreferences.getDefaultFilter(PageFilterKey.TAG_PERFORMERS).findFilter,
                        ) { tags ->
                            PerformerFilterType(tags = tags)
                        }
                    },
                    PagerEntry(DataType.STUDIO) {
                        createStashGridFragment(tag.id, dataType = DataType.STUDIO, null) { tags ->
                            StudioFilterType(tags = tags)
                        }
                    },
                    PagerEntry(getString(R.string.stashapp_sub_tags)) {
                        createStashGridFragment(tag.id, dataType = DataType.TAG, null) { tags ->
                            TagFilterType(parents = tags)
                        }
                    },
                    PagerEntry(getString(R.string.stashapp_parent_tags)) {
                        StashGridFragment(
                            dataType = DataType.TAG,
                            objectFilter =
                                TagFilterType(
                                    children =
                                        createCriterionInput(
                                            false,
                                            tag.id,
                                        ),
                                ),
                        )
                    },
                ).filter { it.title in getUiTabs(requireContext(), DataType.TAG) }
        }
    }

    private fun createStashGridFragment(
        tagId: String,
        dataType: DataType,
        defaultFindFilter: StashFindFilter?,
        createObjectFilter: (Optional<HierarchicalMultiCriterionInput>) -> StashDataFilter,
    ): StashGridFragment {
        val fragment =
            StashGridFragment(
                dataType = dataType,
                findFilter = defaultFindFilter,
                objectFilter = createObjectFilter(createCriterionInput(includeSubTags, tagId)),
            )
        fragment.subContentSwitchInitialIsChecked = includeSubTags
        fragment.subContentText = getString(R.string.stashapp_include_sub_tag_content)
        fragment.subContentSwitchCheckedListener = { isChecked ->
            val newFilter =
                fragment.currentFilter.copy(
                    objectFilter =
                        createObjectFilter(
                            createCriterionInput(isChecked, tagId),
                        ),
                )
            fragment.currentFilter = newFilter
        }
        return fragment
    }

    private fun createCriterionInput(
        subTags: Boolean,
        tagId: String,
    ): Optional.Present<HierarchicalMultiCriterionInput> =
        Optional.present(
            HierarchicalMultiCriterionInput(
                value = Optional.present(listOf(tagId)),
                modifier = CriterionModifier.INCLUDES_ALL,
                depth = Optional.present(if (subTags) -1 else 0),
            ),
        )
}
