package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.GroupRelationshipType
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.Version
import kotlinx.serialization.Serializable

/**
 * A factory to create [StashPagingSource.DataSupplier]s
 */
class DataSupplierFactory(
    val serverVersion: Version,
) {
    /**
     * Create a [StashPagingSource.DataSupplier] for the given [FilterArgs]
     *
     * If the [FilterArgs] has an override, that is always used.
     */
    fun <T : Query.Data, D : StashData, C : Query.Data> create(args: FilterArgs): StashPagingSource.DataSupplier<T, D, C> {
        val filterParser = FilterParser(serverVersion)
        if (args.override != null) {
            return when (args.override) {
                is DataSupplierOverride.PerformerTags -> PerformerTagDataSupplier(args.override.performerId)
                is DataSupplierOverride.GalleryPerformer -> GalleryPerformerDataSupplier(args.override.galleryId)
                is DataSupplierOverride.GalleryTag -> GalleryTagDataSupplier(args.override.galleryId)
                is DataSupplierOverride.GroupTags -> GroupTagDataSupplier(args.override.groupId)
                is DataSupplierOverride.StudioTags -> StudioTagDataSupplier(args.override.studioId)
                is DataSupplierOverride.GroupRelationship ->
                    GroupRelationshipDataSupplier(
                        args.override.groupId,
                        args.override.type,
                    )
            } as StashPagingSource.DataSupplier<T, D, C>
        } else {
            return when (args.dataType) {
                DataType.SCENE ->
                    SceneDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertSceneFilterType(args.objectFilter),
                    )

                DataType.TAG ->
                    TagDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertTagFilterType(args.objectFilter),
                    )

                DataType.STUDIO ->
                    StudioDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertStudioFilterType(args.objectFilter),
                    )

                DataType.MARKER ->
                    MarkerDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertSceneMarkerFilterType(args.objectFilter),
                    )

                DataType.IMAGE ->
                    ImageDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertImageFilterType(args.objectFilter),
                    )

                DataType.GALLERY ->
                    GalleryDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertGalleryFilterType(args.objectFilter),
                    )

                DataType.PERFORMER ->
                    PerformerDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertPerformerFilterType(args.objectFilter),
                    )

                DataType.GROUP ->
                    GroupDataSupplier(
                        args.findFilter?.toFindFilterType(),
                        filterParser.convertGroupFilterType(args.objectFilter),
                    )
            } as StashPagingSource.DataSupplier<T, D, C>
        }
    }
}

/**
 * Represents a [StashPagingSource.DataSupplier] that is atypical.
 *
 * This usually means filtering based on the ID of one [DataType] but querying for a different [DataType] such as the tags on a performer.
 */
@Serializable
sealed interface DataSupplierOverride {
    @Serializable
    data class PerformerTags(
        val performerId: String,
    ) : DataSupplierOverride

    @Serializable
    data class GroupTags(
        val groupId: String,
    ) : DataSupplierOverride

    @Serializable
    data class StudioTags(
        val studioId: String,
    ) : DataSupplierOverride

    @Serializable
    data class GalleryPerformer(
        val galleryId: String,
    ) : DataSupplierOverride

    @Serializable
    data class GalleryTag(
        val galleryId: String,
    ) : DataSupplierOverride

    @Serializable
    data class GroupRelationship(
        val groupId: String,
        val type: GroupRelationshipType,
    ) : DataSupplierOverride
}
