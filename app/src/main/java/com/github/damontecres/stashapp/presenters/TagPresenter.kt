package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.content.Intent
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.FilterListActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.TagActivity
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs
import java.util.EnumMap

class TagPresenter(callback: LongClickCallBack<TagData>? = null) :
    StashPresenter<TagData>(callback) {
    override fun doOnBindViewHolder(
        cardView: StashImageCardView,
        item: TagData,
    ) {
        val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
        dataTypeMap[DataType.SCENE] = item.scene_count
        dataTypeMap[DataType.PERFORMER] = item.performer_count
        dataTypeMap[DataType.MARKER] = item.scene_marker_count
        dataTypeMap[DataType.IMAGE] = item.image_count
        dataTypeMap[DataType.GALLERY] = item.gallery_count

        cardView.setUpExtraRow(dataTypeMap, null)
        cardView.hideOverlayOnSelection = false

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
        if (item.image_path != null) {
            loadImage(cardView, item.image_path)
        }

        if (item.favorite) {
            cardView.setIsFavorite()
        }
    }

    override fun getDefaultLongClickCallBack(cardView: StashImageCardView): LongClickCallBack<TagData> {
        return DefaultTagLongClickCallBack()
    }

    open class DefaultTagLongClickCallBack : LongClickCallBack<TagData> {
        override fun getPopUpItems(
            context: Context,
            item: TagData,
        ): List<PopUpItem> {
            return buildList {
                add(PopUpItem.getDefault(context))
                if (item.child_count > 0) {
                    // TODO, combining two i18n strings is rarely the correct thing
                    val str =
                        context.getString(R.string.go_to) + " " + context.getString(R.string.stashapp_sub_tags)
                    add(PopUpItem(POPUP_GOTO_WITH_SUB_ID, str))
                }
                if (item.parent_count > 0) {
                    val str = context.getString(R.string.stashapp_parent_tags)
                    add(PopUpItem(POPUP_PARENTS_ID, str))
                }
                if (item.child_count > 0) {
                    val str = context.getString(R.string.stashapp_sub_tags)
                    add(PopUpItem(POPUP_CHILDREN_ID, str))
                }
            }
        }

        override fun onItemLongClick(
            context: Context,
            item: TagData,
            popUpItem: PopUpItem,
        ) {
            when (popUpItem.id) {
                PopUpItem.DEFAULT_ID -> {
                    val intent = Intent(context, TagActivity::class.java)
                    intent.putExtra("tagId", item.id)
                    intent.putExtra("tagName", item.name)
                    intent.putExtra("includeSubTags", false)
                    context.startActivity(intent)
                }

                POPUP_GOTO_WITH_SUB_ID -> {
                    val intent = Intent(context, TagActivity::class.java)
                    intent.putExtra("tagId", item.id)
                    intent.putExtra("tagName", item.name)
                    intent.putExtra("includeSubTags", true)
                    context.startActivity(intent)
                }

                POPUP_PARENTS_ID -> {
                    val name = context.getString(R.string.stashapp_parent_of, item.name)
                    val intent =
                        Intent(context, FilterListActivity::class.java)
                            .putExtra(
                                FilterListActivity.INTENT_FILTER_ARGS,
                                FilterArgs(
                                    dataType = DataType.TAG,
                                    name = name,
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
                            )
                    context.startActivity(intent)
                }

                POPUP_CHILDREN_ID -> {
                    val name = context.getString(R.string.stashapp_sub_tag_of, item.name)
                    val intent =
                        Intent(context, FilterListActivity::class.java)
                            .putExtra(
                                FilterListActivity.INTENT_SCROLL_NEXT_PAGE,
                                FilterArgs(
                                    dataType = DataType.TAG,
                                    name = name,
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
                            )
                    context.startActivity(intent)
                }
            }
        }
    }

    companion object {
        private const val TAG = "TagPresenter"

        const val CARD_WIDTH = 250
        const val CARD_HEIGHT = 250

        const val POPUP_PARENTS_ID = 100L
        const val POPUP_CHILDREN_ID = 101L
        const val POPUP_GOTO_WITH_SUB_ID = 102L
    }
}
