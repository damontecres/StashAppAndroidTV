package com.github.damontecres.stashapp

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.leanback.widget.ClassPresenterSelector
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.views.models.PerformerViewModel

/**
 * Main [TabbedFragment] for a performers which includes [PerformerDetailsFragment] and other tabs
 */
class PerformerFragment : TabbedFragment(DataType.PERFORMER.name) {
    private val viewModel: PerformerViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        serverViewModel.currentServer.observe(viewLifecycleOwner) {
            viewModel.init(requireArguments())
        }

        serverViewModel
            .withLiveData(viewModel.item)
            .observe(viewLifecycleOwner) { (server, performer) ->
                if (performer == null) {
                    Toast
                        .makeText(
                            requireContext(),
                            "Performer '${viewModel.itemId}' not found",
                            Toast.LENGTH_LONG,
                        ).show()
                    serverViewModel.navigationManager.goBack()
                    return@observe
                }
                tabViewModel.title.value =
                    SpannableString("${performer.name} ${performer.disambiguation}").apply {
                        val start = performer.name.length + 1
                        val end = length
                        setSpan(RelativeSizeSpan(.60f), start, end, 0)
                        setSpan(ForegroundColorSpan(Color.GRAY), start, end, 0)
                    }

                val performers =
                    Optional.present(
                        MultiCriterionInput(
                            value = Optional.present(listOf(performer.id)),
                            modifier = CriterionModifier.INCLUDES_ALL,
                        ),
                    )

                if (!tabViewModel.tabs.isInitialized) {
                    tabViewModel.tabs.value =
                        listOf(
                            StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_details)) {
                                PerformerDetailsFragment()
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.SCENE) {
                                StashGridControlsFragment(
                                    dataType = DataType.SCENE,
                                    findFilter = server.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_SCENES).findFilter,
                                    objectFilter = SceneFilterType(performers = performers),
                                )
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.GALLERY) {
                                StashGridControlsFragment(
                                    dataType = DataType.GALLERY,
                                    findFilter = server.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_GALLERIES).findFilter,
                                    objectFilter = GalleryFilterType(performers = performers),
                                )
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.IMAGE) {
                                StashGridControlsFragment(
                                    dataType = DataType.IMAGE,
                                    findFilter = server.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_IMAGES).findFilter,
                                    objectFilter = ImageFilterType(performers = performers),
                                )
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.GROUP) {
                                StashGridControlsFragment(
                                    dataType = DataType.GROUP,
                                    findFilter = server.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_GROUPS).findFilter,
                                    objectFilter = GroupFilterType(performers = performers),
                                )
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.TAG) {
                                val presenter =
                                    ClassPresenterSelector()
                                        .addClassPresenter(
                                            TagData::class.java,
                                            TagPresenter(performersWithTagLongClickCallback()),
                                        )
                                val fragment =
                                    StashGridControlsFragment(
                                        FilterArgs(
                                            dataType = DataType.TAG,
                                            override = DataSupplierOverride.PerformerTags(performer.id),
                                        ),
                                    )
                                fragment.presenterSelector = presenter
                                fragment
                            },
                            StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_appears_with)) {
                                val presenter =
                                    ClassPresenterSelector()
                                        .addClassPresenter(
                                            PerformerData::class.java,
                                            PerformerPresenter(
                                                performTogetherLongClickCallback(
                                                    performer,
                                                ),
                                            ),
                                        )
                                val fragment =
                                    StashGridControlsFragment(
                                        dataType = DataType.PERFORMER,
                                        findFilter =
                                            server.serverPreferences
                                                .getDefaultFilter(
                                                    PageFilterKey.PERFORMER_APPEARS_WITH,
                                                ).findFilter,
                                        objectFilter =
                                            PerformerFilterType(
                                                performers =
                                                    Optional.present(
                                                        MultiCriterionInput(
                                                            value = Optional.present(listOf(performer.id)),
                                                            modifier = CriterionModifier.INCLUDES_ALL,
                                                        ),
                                                    ),
                                            ),
                                    )
                                fragment.presenterSelector = presenter
                                fragment
                            },
                            StashFragmentPagerAdapter.PagerEntry(DataType.MARKER) {
                                StashGridControlsFragment(
                                    dataType = DataType.MARKER,
                                    objectFilter = SceneMarkerFilterType(performers = performers),
                                )
                            },
                        ).filter { it.title in getUiTabs(requireContext(), DataType.PERFORMER) }
                }
            }
    }

    private fun performTogetherLongClickCallback(performer: PerformerData) =
        StashPresenter
            .LongClickCallBack<PerformerData>(
                StashPresenter.PopUpItem.DEFAULT to StashPresenter.PopUpAction { cardView, _ -> cardView.performClick() },
            ).addAction(StashPresenter.PopUpItem(1L, "View scenes together")) { _, item ->
                val performerIds = listOf(performer.id, item.id)
                val name = "${performer.name} & ${item.name}"
                val filter =
                    FilterArgs(
                        dataType = DataType.SCENE,
                        name = name,
                        objectFilter =
                            SceneFilterType(
                                performers =
                                    Optional.present(
                                        MultiCriterionInput(
                                            value = Optional.present(performerIds),
                                            modifier = CriterionModifier.INCLUDES_ALL,
                                        ),
                                    ),
                            ),
                    )
                serverViewModel.navigationManager.navigate(Destination.Filter(filter))
            }

    private fun performersWithTagLongClickCallback() =
        StashPresenter
            .LongClickCallBack<TagData>(
                StashPresenter.PopUpItem.DEFAULT to StashPresenter.PopUpAction { cardView, _ -> cardView.performClick() },
            ).addAction(
                StashPresenter.PopUpItem(
                    1,
                    "View performers with this tag",
                ),
            ) { _, item ->
                val name = "Performers with ${item.name}"
                val filter =
                    FilterArgs(
                        dataType = DataType.PERFORMER,
                        name = name,
                        objectFilter =
                            PerformerFilterType(
                                tags =
                                    Optional.present(
                                        HierarchicalMultiCriterionInput(
                                            value = Optional.present(listOf(item.id)),
                                            modifier = CriterionModifier.INCLUDES_ALL,
                                            depth = Optional.absent(),
                                        ),
                                    ),
                            ),
                    )
                serverViewModel.navigationManager.navigate(Destination.Filter(filter))
            }
}
