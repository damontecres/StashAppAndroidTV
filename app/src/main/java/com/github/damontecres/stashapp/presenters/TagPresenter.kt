package com.github.damontecres.stashapp.presenters

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.presenters.StashPresenter.PopUpAction
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.StashServer
import java.util.EnumMap

class TagPresenter(
    server: StashServer,
    callback: LongClickCallBack<TagData>? = null,
) : StashPresenter<TagData>(server, callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: TagData,
    ) {
        cardView.blackImageBackground = false

        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = item.scene_count
        dataTypeMap[DataType.PERFORMER] = item.performer_count
        dataTypeMap[DataType.MARKER] = item.scene_marker_count
        dataTypeMap[DataType.IMAGE] = item.image_count
        dataTypeMap[DataType.GALLERY] = item.gallery_count

        cardView.setUpExtraRow(dataTypeMap, null)
//        cardView.hideOverlayOnSelection = false

        cardView.titleText = item.name
        cardView.contentText = item.description
        if (item.child_count > 0) {
            val parentText =
                cardView.context.getString(
                    R.string.stashapp_parent_of,
                    item.child_count.toString(),
                )
            cardView.setTextOverlayText(StashImageCardView.OverlayPosition.TOP_LEFT, parentText)
        }
        if (item.parent_count > 0) {
            val childText =
                cardView.context.getString(
                    R.string.stashapp_sub_tag_of,
                    item.parent_count.toString(),
                )
            cardView.setTextOverlayText(StashImageCardView.OverlayPosition.BOTTOM_LEFT, childText)
        }
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        loadImage(cardView, item.image_path, defaultDrawable = R.drawable.default_tag)

        if (item.favorite) {
            cardView.setIsFavorite()
        }
    }

    override fun getDefaultLongClickCallBack(): LongClickCallBack<TagData> =
        LongClickCallBack<TagData>(PopUpItem.DEFAULT to PopUpAction { cardView, _ -> cardView.performClick() })
            .addAction(
                PopUpItem(POPUP_PARENTS_ID, R.string.stashapp_parent_tags),
                { it.parent_count > 0 },
            ) { cardView, item ->
                StashApplication.navigationManager.navigate(
                    Destination.Filter(
                        FilterArgs(
                            dataType = DataType.TAG,
                            name =
                                cardView.context.getString(
                                    R.string.stashapp_parent_of,
                                    item.name,
                                ),
                            objectFilter =
                                TagFilterType(
                                    children =
                                        Optional.present(
                                            HierarchicalMultiCriterionInput(
                                                value = Optional.present(listOf(item.id)),
                                                modifier = CriterionModifier.INCLUDES,
                                                depth = Optional.present(-1),
                                            ),
                                        ),
                                ),
                        ),
                        false,
                    ),
                )
            }.addAction(
                PopUpItem(POPUP_CHILDREN_ID, R.string.stashapp_sub_tags),
                { it.child_count > 0 },
            ) { cardView, item ->
                StashApplication.navigationManager.navigate(
                    Destination.Filter(
                        FilterArgs(
                            dataType = DataType.TAG,
                            name =
                                cardView.context.getString(
                                    R.string.stashapp_sub_tag_of,
                                    item.name,
                                ),
                            objectFilter =
                                TagFilterType(
                                    parents =
                                        Optional.present(
                                            HierarchicalMultiCriterionInput(
                                                value = Optional.present(listOf(item.id)),
                                                modifier = CriterionModifier.INCLUDES,
                                                depth = Optional.present(-1),
                                            ),
                                        ),
                                ),
                        ),
                        false,
                    ),
                )
            }

    companion object {
        private const val TAG = "TagPresenter"

        const val CARD_WIDTH = 250
        const val CARD_HEIGHT = 250

        const val POPUP_PARENTS_ID = 100L
        const val POPUP_CHILDREN_ID = 101L
    }
}
