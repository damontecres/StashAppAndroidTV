package com.github.damontecres.stashapp

import androidx.fragment.app.FragmentManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter

class StudioActivity : TabbedGridFragmentActivity() {
    override fun getTitleText(): CharSequence? {
        return intent.getStringExtra("studioName")
    }

    override fun getPagerAdapter(): StashFragmentPagerAdapter {
        val studioId = this.intent.getIntExtra("studioId", -1)
        val tabTitles =
            mutableListOf(
                StashFragmentPagerAdapter.PagerEntry(DataType.SCENE),
                StashFragmentPagerAdapter.PagerEntry(DataType.GALLERY),
                StashFragmentPagerAdapter.PagerEntry(DataType.IMAGE),
                StashFragmentPagerAdapter.PagerEntry(DataType.PERFORMER),
                StashFragmentPagerAdapter.PagerEntry(DataType.MOVIE),
                StashFragmentPagerAdapter.PagerEntry(
                    getString(R.string.stashapp_subsidiary_studios),
                    DataType.STUDIO,
                ),
            )
        return StudioPagerAdapter(tabTitles, studioId.toString(), supportFragmentManager)
    }

    class StudioPagerAdapter(
        tabs: MutableList<StashFragmentPagerAdapter.PagerEntry>,
        private val studioId: String,
        fm: FragmentManager,
    ) :
        StashFragmentPagerAdapter(tabs, fm) {
        override fun getFragment(position: Int): StashGridFragment2 {
            val studios =
                Optional.present(
                    HierarchicalMultiCriterionInput(
                        value = Optional.present(listOf(studioId)),
                        modifier = CriterionModifier.INCLUDES,
                    ),
                )
            return if (position == 0) {
                StashGridFragment2(
                    dataType = DataType.SCENE,
                    objectFilter = SceneFilterType(studios = studios),
                )
            } else if (position == 1) {
                StashGridFragment2(
                    dataType = DataType.GALLERY,
                    objectFilter = GalleryFilterType(studios = studios),
                )
            } else if (position == 2) {
                StashGridFragment2(
                    dataType = DataType.IMAGE,
                    objectFilter = ImageFilterType(studios = studios),
                )
            } else if (position == 3) {
                StashGridFragment2(
                    dataType = DataType.PERFORMER,
                    objectFilter = PerformerFilterType(studios = studios),
                )
            } else if (position == 4) {
                StashGridFragment2(
                    dataType = DataType.MOVIE,
                    objectFilter = MovieFilterType(studios = studios),
                )
            } else if (position == 5) {
                StashGridFragment2(
                    dataType = DataType.STUDIO,
                    objectFilter =
                        StudioFilterType(
                            parents =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(studioId)),
                                        modifier = CriterionModifier.INCLUDES,
                                    ),
                                ),
                        ),
                )
            } else {
                throw IllegalStateException()
            }
        }
    }
}
