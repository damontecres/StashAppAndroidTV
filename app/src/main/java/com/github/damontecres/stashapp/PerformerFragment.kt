package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
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
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.util.putFilterArgs
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import com.github.damontecres.stashapp.views.models.PerformerViewModel

/**
 * Main [TabbedFragment] for a performers which includes [PerformerDetailsFragment] and other tabs
 */
class PerformerFragment : TabbedFragment(DataType.PERFORMER.name) {
    private val viewModel: PerformerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.init(requireArguments().getDestination<Destination.Item>().id)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.performer.observe(viewLifecycleOwner) { performer ->
            if (performer == null) {
                Toast
                    .makeText(
                        requireContext(),
                        "No performer found with ID ${requireArguments().getDestination<Destination.Item>().id}",
                        Toast.LENGTH_LONG,
                    ).show()
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

            val server = serverViewModel.requireServer()
            tabViewModel.tabs.value =
                listOf(
                    StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_details)) {
                        PerformerDetailsFragment()
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.SCENE) {
                        StashGridFragment(
                            dataType = DataType.SCENE,
                            findFilter = server.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_SCENES).findFilter,
                            objectFilter = SceneFilterType(performers = performers),
                        )
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.GALLERY) {
                        StashGridFragment(
                            dataType = DataType.GALLERY,
                            findFilter = server.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_GALLERIES).findFilter,
                            objectFilter = GalleryFilterType(performers = performers),
                        )
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.IMAGE) {
                        StashGridFragment(
                            dataType = DataType.IMAGE,
                            findFilter = server.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_IMAGES).findFilter,
                            objectFilter = ImageFilterType(performers = performers),
                        ).withImageGridClickListener()
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.GROUP) {
                        StashGridFragment(
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
                                    TagPresenter(PerformersWithTagLongClickCallback()),
                                )
                        val fragment =
                            StashGridFragment(
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
                                        PerformTogetherLongClickCallback(
                                            performer,
                                        ),
                                    ),
                                )
                        val fragment =
                            StashGridFragment(
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
                        StashGridFragment(
                            dataType = DataType.MARKER,
                            objectFilter = SceneMarkerFilterType(performers = performers),
                        )
                    },
                ).filter { it.title in getUiTabs(requireContext(), DataType.PERFORMER) }
        }
    }

    private class PerformTogetherLongClickCallback(
        val performer: PerformerData,
    ) : StashPresenter.LongClickCallBack<PerformerData> {
        override fun getPopUpItems(
            context: Context,
            item: PerformerData,
        ): List<StashPresenter.PopUpItem> =
            listOf(
                StashPresenter.PopUpItem.getDefault(context),
                StashPresenter.PopUpItem(1, "View scenes together"),
            )

        override fun onItemLongClick(
            context: Context,
            item: PerformerData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            when (popUpItem.id) {
                0L -> {
                    StashItemViewClickListener(context).onItemClicked(item)
                }

                1L -> {
                    val performerIds = listOf(performer.id, item.id)
                    val name = "${performer.name} & ${item.name}"
                    val intent =
                        Intent(context, FilterListActivity::class.java)
                            .putFilterArgs(
                                FilterListActivity.INTENT_FILTER_ARGS,
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
                                ),
                            )
                    context.startActivity(intent)
                }
            }
        }
    }

    private class PerformersWithTagLongClickCallback : StashPresenter.LongClickCallBack<TagData> {
        override fun getPopUpItems(
            context: Context,
            item: TagData,
        ): List<StashPresenter.PopUpItem> =
            listOf(
                StashPresenter.PopUpItem.getDefault(context),
                StashPresenter.PopUpItem(1, "View performers with this tag"),
            )

        override fun onItemLongClick(
            context: Context,
            item: TagData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            when (popUpItem.id) {
                StashPresenter.PopUpItem.DEFAULT_ID -> {
                    StashItemViewClickListener(context).onItemClicked(item)
                }

                1L -> {
                    val name = "Performers with ${item.name}"
                    val intent =
                        Intent(context, FilterListActivity::class.java)
                            .putFilterArgs(
                                FilterListActivity.INTENT_FILTER_ARGS,
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
                                ),
                            )
                    context.startActivity(intent)
                }
            }
        }
    }
}
