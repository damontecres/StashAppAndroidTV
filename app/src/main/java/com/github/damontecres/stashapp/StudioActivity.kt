package com.github.damontecres.stashapp

import androidx.fragment.app.FragmentManager
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
            TODO()
//            return if (position == 0) {
//                StashGridFragment(
//                    SceneComparator,
//                    SceneDataSupplier(
//                        items[position].getFindFilter(),
//                        SceneFilterType(
//                            studios =
//                                Optional.present(
//                                    HierarchicalMultiCriterionInput(
//                                        value = Optional.present(listOf(studioId)),
//                                        modifier = CriterionModifier.INCLUDES,
//                                    ),
//                                ),
//                        ),
//                    ),
//                )
//            } else if (position == 1) {
//                StashGridFragment(
//                    GalleryComparator,
//                    GalleryDataSupplier(
//                        items[position].getFindFilter(),
//                        GalleryFilterType(
//                            studios =
//                                Optional.present(
//                                    HierarchicalMultiCriterionInput(
//                                        value = Optional.present(listOf(studioId)),
//                                        modifier = CriterionModifier.INCLUDES,
//                                    ),
//                                ),
//                        ),
//                    ),
//                )
//            } else if (position == 2) {
//                StashGridFragment(
//                    ImageComparator,
//                    ImageDataSupplier(
//                        items[position].getFindFilter(),
//                        ImageFilterType(
//                            studios =
//                                Optional.present(
//                                    HierarchicalMultiCriterionInput(
//                                        value = Optional.present(listOf(studioId)),
//                                        modifier = CriterionModifier.INCLUDES,
//                                    ),
//                                ),
//                        ),
//                    ),
//                )
//            } else if (position == 3) {
//                StashGridFragment(
//                    PerformerComparator,
//                    PerformerDataSupplier(
//                        items[position].getFindFilter(),
//                        PerformerFilterType(
//                            studios =
//                                Optional.present(
//                                    HierarchicalMultiCriterionInput(
//                                        value = Optional.present(listOf(studioId)),
//                                        modifier = CriterionModifier.INCLUDES,
//                                    ),
//                                ),
//                        ),
//                    ),
//                )
//            } else if (position == 4) {
//                StashGridFragment(
//                    MovieComparator,
//                    MovieDataSupplier(
//                        items[position].getFindFilter(),
//                        MovieFilterType(
//                            studios =
//                                Optional.present(
//                                    HierarchicalMultiCriterionInput(
//                                        value = Optional.present(listOf(studioId)),
//                                        modifier = CriterionModifier.INCLUDES,
//                                    ),
//                                ),
//                        ),
//                    ),
//                )
//            } else if (position == 5) {
//                StashGridFragment(
//                    StudioComparator,
//                    StudioDataSupplier(
//                        items[position].getFindFilter(),
//                        StudioFilterType(
//                            parents =
//                                Optional.present(
//                                    MultiCriterionInput(
//                                        value = Optional.present(listOf(studioId)),
//                                        modifier = CriterionModifier.INCLUDES,
//                                    ),
//                                ),
//                        ),
//                    ),
//                )
//            } else {
//                throw IllegalStateException()
//            }
        }
    }
}
