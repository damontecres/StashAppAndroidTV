package com.github.damontecres.stashapp

import androidx.fragment.app.FragmentManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter.PagerEntry
import com.github.damontecres.stashapp.util.getUiTabs

class TagFragment : TabbedFragment() {
    override fun getTitleText(): String? {
        return requireActivity().intent.getStringExtra("tagName")
    }

    override fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter {
        val tagId = requireActivity().intent.getStringExtra("tagId")!!
        val includeSubTags = requireActivity().intent.getBooleanExtra("includeSubTags", false)
        val depth = Optional.present(if (includeSubTags) -1 else 0)
        val tags =
            Optional.present(
                HierarchicalMultiCriterionInput(
                    value = Optional.present(listOf(tagId)),
                    modifier = CriterionModifier.INCLUDES_ALL,
                    depth = depth,
                ),
            )
        val items =
            listOf(
                PagerEntry(DataType.SCENE) {
                    StashGridFragment(
                        dataType = DataType.SCENE,
                        objectFilter = SceneFilterType(tags = tags),
                    )
                },
                PagerEntry(DataType.GALLERY) {
                    StashGridFragment(
                        dataType = DataType.GALLERY,
                        objectFilter = GalleryFilterType(tags = tags),
                    )
                },
                PagerEntry(DataType.IMAGE) {
                    StashGridFragment(
                        dataType = DataType.IMAGE,
                        objectFilter = ImageFilterType(tags = tags),
                    ).withImageGridClickListener()
                },
                PagerEntry(DataType.MARKER) {
                    StashGridFragment(
                        dataType = DataType.MARKER,
                        objectFilter = SceneMarkerFilterType(tags = tags),
                    )
                },
                PagerEntry(DataType.PERFORMER) {
                    StashGridFragment(
                        dataType = DataType.PERFORMER,
                        objectFilter = PerformerFilterType(tags = tags),
                    )
                },
                PagerEntry(getString(R.string.stashapp_sub_tags)) {
                    StashGridFragment(
                        dataType = DataType.TAG,
                        objectFilter = TagFilterType(parents = tags),
                    )
                },
            ).filter { it.title in getUiTabs(requireContext(), DataType.TAG) }

        return StashFragmentPagerAdapter(items, fm)
    }
}
