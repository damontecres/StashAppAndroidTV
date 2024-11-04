package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentManager
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
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter.PagerEntry
import com.github.damontecres.stashapp.util.getUiTabs

class TagFragment : TabbedFragment(DataType.TAG.name) {
    private lateinit var tagId: String
    private var includeSubTags = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        includeSubTags = requireActivity().intent.getBooleanExtra("includeSubTags", false)
        tagId = requireActivity().intent.getStringExtra("tagId")!!
    }

    override fun getTitleText(): String? {
        return requireActivity().intent.getStringExtra("tagName")
    }

    override fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter {
        val items =
            listOf(
                PagerEntry(getString(R.string.stashapp_details)) {
                    TagDetailsFragment()
                },
                PagerEntry(DataType.SCENE) {
                    createStashGridFragment(dataType = DataType.SCENE) { tags ->
                        SceneFilterType(tags = tags)
                    }
                },
                PagerEntry(DataType.GALLERY) {
                    createStashGridFragment(dataType = DataType.GALLERY) { tags ->
                        GalleryFilterType(tags = tags)
                    }
                },
                PagerEntry(DataType.IMAGE) {
                    createStashGridFragment(dataType = DataType.IMAGE) { tags ->
                        ImageFilterType(tags = tags)
                    }.withImageGridClickListener()
                },
                PagerEntry(DataType.MARKER) {
                    createStashGridFragment(dataType = DataType.MARKER) { tags ->
                        SceneMarkerFilterType(tags = tags)
                    }
                },
                PagerEntry(DataType.PERFORMER) {
                    createStashGridFragment(dataType = DataType.PERFORMER) { tags ->
                        PerformerFilterType(tags = tags)
                    }
                },
                PagerEntry(DataType.STUDIO) {
                    createStashGridFragment(dataType = DataType.STUDIO) { tags ->
                        StudioFilterType(tags = tags)
                    }
                },
                PagerEntry(getString(R.string.stashapp_sub_tags)) {
                    createStashGridFragment(dataType = DataType.TAG) { tags ->
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

        return StashFragmentPagerAdapter(items, fm)
    }

    private fun createStashGridFragment(
        dataType: DataType,
        createObjectFilter: (Optional<HierarchicalMultiCriterionInput>) -> StashDataFilter,
    ): StashGridFragment {
        val fragment =
            StashGridFragment(
                dataType = dataType,
                objectFilter = createObjectFilter(createCriterionInput(includeSubTags)),
            )
        fragment.subTagSwitchInitialIsChecked = includeSubTags
        fragment.subTagSwitchCheckedListener = { isChecked ->
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

    private fun createCriterionInput(subTags: Boolean): Optional.Present<HierarchicalMultiCriterionInput> {
        return Optional.present(
            HierarchicalMultiCriterionInput(
                value = Optional.present(listOf(tagId)),
                modifier = CriterionModifier.INCLUDES_ALL,
                depth = Optional.present(if (subTags) -1 else 0),
            ),
        )
    }
}
