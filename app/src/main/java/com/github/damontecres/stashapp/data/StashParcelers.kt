package com.github.damontecres.stashapp.data

import android.os.Parcel
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<SceneFilterType?, SceneFilterTypeParceler>()
data class SceneFilterTypeHolder(override val value: SceneFilterType?) :
    FilterHolder<SceneFilterType>

object SceneFilterTypeParceler : Parceler<SceneFilterType?> {
    override fun create(parcel: Parcel): SceneFilterType? {
        if (parcel.readByte() == ABSENT) {
            return null
        } else {
            return SceneFilterType(
                AND = Optional.presentIfNotNull(create(parcel)),
                OR = Optional.presentIfNotNull(create(parcel)),
                NOT = Optional.presentIfNotNull(create(parcel)),
                id = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                title = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                code = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                details = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                director = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                oshash = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                checksum = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                phash = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                phash_distance = Optional.presentIfNotNull(readPhashDistanceCriterionInput(parcel)),
                path = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                file_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                rating100 = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                organized = Optional.presentIfNotNull(readBoolean(parcel)),
                o_counter = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                duplicated = Optional.presentIfNotNull(readPHashDuplicationCriterionInput(parcel)),
                resolution = Optional.presentIfNotNull(readResolutionCriterionInput(parcel)),
                orientation = Optional.presentIfNotNull(readOrientationCriterionInput(parcel)),
                framerate = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                bitrate = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                video_codec = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                audio_codec = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                duration = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                has_markers = Optional.presentIfNotNull(readString(parcel)),
                is_missing = Optional.presentIfNotNull(readString(parcel)),
                studios = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                movies = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                galleries = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                tags = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                tag_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                performer_tags = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                performer_favorite = Optional.presentIfNotNull(readBoolean(parcel)),
                performer_age = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                performers = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                performer_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                stash_id_endpoint = Optional.presentIfNotNull(readStashIDCriterionInput(parcel)),
                url = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                interactive = Optional.presentIfNotNull(readBoolean(parcel)),
                interactive_speed = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                captions = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                resume_time = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                play_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                play_duration = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                last_played_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                date = Optional.presentIfNotNull(readDateCriterionInput(parcel)),
                created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
            )
        }
    }

    override fun SceneFilterType?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        if (this == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)

            AND.getOrNull().write(parcel, flags)
            OR.getOrNull().write(parcel, flags)
            NOT.getOrNull().write(parcel, flags)
            writeIntCriterionInput(parcel, id.getOrNull())
            writeStringCriterionInput(parcel, title.getOrNull())
            writeStringCriterionInput(parcel, code.getOrNull())
            writeStringCriterionInput(parcel, details.getOrNull())
            writeStringCriterionInput(parcel, director.getOrNull())
            writeStringCriterionInput(parcel, oshash.getOrNull())
            writeStringCriterionInput(parcel, checksum.getOrNull())
            writeStringCriterionInput(parcel, phash.getOrNull())
            writePhashDistanceCriterionInput(parcel, phash_distance.getOrNull())
            writeStringCriterionInput(parcel, path.getOrNull())
            writeIntCriterionInput(parcel, file_count.getOrNull())
            writeIntCriterionInput(parcel, rating100.getOrNull())
            writeBoolean(parcel, organized.getOrNull())
            writeIntCriterionInput(parcel, o_counter.getOrNull())
            writePHashDuplicationCriterionInput(parcel, duplicated.getOrNull())
            writeResolutionCriterionInput(parcel, resolution.getOrNull())
            writeOrientationCriterionInput(parcel, orientation.getOrNull())
            writeIntCriterionInput(parcel, framerate.getOrNull())
            writeIntCriterionInput(parcel, bitrate.getOrNull())
            writeStringCriterionInput(parcel, video_codec.getOrNull())
            writeStringCriterionInput(parcel, audio_codec.getOrNull())
            writeIntCriterionInput(parcel, duration.getOrNull())
            writeString(parcel, has_markers.getOrNull())
            writeString(parcel, is_missing.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, studios.getOrNull())
            writeMultiCriterionInput(parcel, movies.getOrNull())
            writeMultiCriterionInput(parcel, galleries.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, tags.getOrNull())
            writeIntCriterionInput(parcel, tag_count.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, performer_tags.getOrNull())
            writeBoolean(parcel, performer_favorite.getOrNull())
            writeIntCriterionInput(parcel, performer_age.getOrNull())
            writeMultiCriterionInput(parcel, performers.getOrNull())
            writeIntCriterionInput(parcel, performer_count.getOrNull())
            writeStashIDCriterionInput(parcel, stash_id_endpoint.getOrNull())
            writeStringCriterionInput(parcel, url.getOrNull())
            writeBoolean(parcel, interactive.getOrNull())
            writeIntCriterionInput(parcel, interactive_speed.getOrNull())
            writeStringCriterionInput(parcel, captions.getOrNull())
            writeIntCriterionInput(parcel, resume_time.getOrNull())
            writeIntCriterionInput(parcel, play_count.getOrNull())
            writeIntCriterionInput(parcel, play_duration.getOrNull())
            writeTimestampCriterionInput(parcel, last_played_at.getOrNull())
            writeDateCriterionInput(parcel, date.getOrNull())
            writeTimestampCriterionInput(parcel, created_at.getOrNull())
            writeTimestampCriterionInput(parcel, updated_at.getOrNull())
        }
    }
}

@Parcelize
@TypeParceler<PerformerFilterType?, PerformerFilterTypeParceler>()
data class PerformerFilterTypeHolder(override val value: PerformerFilterType?) :
    FilterHolder<PerformerFilterType>

object PerformerFilterTypeParceler : Parceler<PerformerFilterType?> {
    override fun create(parcel: Parcel): PerformerFilterType? {
        if (parcel.readByte() == ABSENT) {
            return null
        } else {
            return PerformerFilterType(
                AND = Optional.presentIfNotNull(create(parcel)),
                OR = Optional.presentIfNotNull(create(parcel)),
                NOT = Optional.presentIfNotNull(create(parcel)),
                name = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                disambiguation = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                details = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                filter_favorites = Optional.presentIfNotNull(readBoolean(parcel)),
                birth_year = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                age = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                ethnicity = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                country = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                eye_color = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                height_cm = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                measurements = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                fake_tits = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                penis_length = Optional.presentIfNotNull(readFloatCriterionInput(parcel)),
                circumcised = Optional.presentIfNotNull(readCircumcisionCriterionInput(parcel)),
                career_length = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                tattoos = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                piercings = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                aliases = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                gender = Optional.presentIfNotNull(readGenderCriterionInput(parcel)),
                is_missing = Optional.presentIfNotNull(readString(parcel)),
                tags = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                tag_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                scene_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                image_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                gallery_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                play_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                o_counter = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                stash_id_endpoint = Optional.presentIfNotNull(readStashIDCriterionInput(parcel)),
                rating100 = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                url = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                hair_color = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                weight = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                death_year = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                studios = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                performers = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                ignore_auto_tag = Optional.presentIfNotNull(readBoolean(parcel)),
                birthdate = Optional.presentIfNotNull(readDateCriterionInput(parcel)),
                death_date = Optional.presentIfNotNull(readDateCriterionInput(parcel)),
                created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
            )
        }
    }

    override fun PerformerFilterType?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        if (this == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)

            AND.getOrNull().write(parcel, flags)
            OR.getOrNull().write(parcel, flags)
            NOT.getOrNull().write(parcel, flags)
            writeStringCriterionInput(parcel, name.getOrNull())
            writeStringCriterionInput(parcel, disambiguation.getOrNull())
            writeStringCriterionInput(parcel, details.getOrNull())
            writeBoolean(parcel, filter_favorites.getOrNull())
            writeIntCriterionInput(parcel, birth_year.getOrNull())
            writeIntCriterionInput(parcel, age.getOrNull())
            writeStringCriterionInput(parcel, ethnicity.getOrNull())
            writeStringCriterionInput(parcel, country.getOrNull())
            writeStringCriterionInput(parcel, eye_color.getOrNull())
            writeIntCriterionInput(parcel, height_cm.getOrNull())
            writeStringCriterionInput(parcel, measurements.getOrNull())
            writeStringCriterionInput(parcel, fake_tits.getOrNull())
            writeFloatCriterionInput(parcel, penis_length.getOrNull())
            writeCircumcisionCriterionInput(parcel, circumcised.getOrNull())
            writeStringCriterionInput(parcel, career_length.getOrNull())
            writeStringCriterionInput(parcel, tattoos.getOrNull())
            writeStringCriterionInput(parcel, piercings.getOrNull())
            writeStringCriterionInput(parcel, aliases.getOrNull())
            writeGenderCriterionInput(parcel, gender.getOrNull())
            writeString(parcel, is_missing.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, tags.getOrNull())
            writeIntCriterionInput(parcel, tag_count.getOrNull())
            writeIntCriterionInput(parcel, scene_count.getOrNull())
            writeIntCriterionInput(parcel, image_count.getOrNull())
            writeIntCriterionInput(parcel, gallery_count.getOrNull())
            writeIntCriterionInput(parcel, play_count.getOrNull())
            writeIntCriterionInput(parcel, o_counter.getOrNull())
            writeStashIDCriterionInput(parcel, stash_id_endpoint.getOrNull())
            writeIntCriterionInput(parcel, rating100.getOrNull())
            writeStringCriterionInput(parcel, url.getOrNull())
            writeStringCriterionInput(parcel, hair_color.getOrNull())
            writeIntCriterionInput(parcel, weight.getOrNull())
            writeIntCriterionInput(parcel, death_year.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, studios.getOrNull())
            writeMultiCriterionInput(parcel, performers.getOrNull())
            writeBoolean(parcel, ignore_auto_tag.getOrNull())
            writeDateCriterionInput(parcel, birthdate.getOrNull())
            writeDateCriterionInput(parcel, death_date.getOrNull())
            writeTimestampCriterionInput(parcel, created_at.getOrNull())
            writeTimestampCriterionInput(parcel, updated_at.getOrNull())
        }
    }
}

@Parcelize
@TypeParceler<StudioFilterType?, StudioFilterTypeParceler>()
data class StudioFilterTypeHolder(override val value: StudioFilterType?) :
    FilterHolder<StudioFilterType>

object StudioFilterTypeParceler : Parceler<StudioFilterType?> {
    override fun create(parcel: Parcel): StudioFilterType? {
        if (parcel.readByte() == ABSENT) {
            return null
        } else {
            return StudioFilterType(
                AND = Optional.presentIfNotNull(create(parcel)),
                OR = Optional.presentIfNotNull(create(parcel)),
                NOT = Optional.presentIfNotNull(create(parcel)),
                name = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                details = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                parents = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                stash_id_endpoint = Optional.presentIfNotNull(readStashIDCriterionInput(parcel)),
                is_missing = Optional.presentIfNotNull(readString(parcel)),
                rating100 = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                favorite = Optional.presentIfNotNull(readBoolean(parcel)),
                scene_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                image_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                gallery_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                url = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                aliases = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                child_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                ignore_auto_tag = Optional.presentIfNotNull(readBoolean(parcel)),
                created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
            )
        }
    }

    override fun StudioFilterType?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        if (this == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)

            AND.getOrNull().write(parcel, flags)
            OR.getOrNull().write(parcel, flags)
            NOT.getOrNull().write(parcel, flags)
            writeStringCriterionInput(parcel, name.getOrNull())
            writeStringCriterionInput(parcel, details.getOrNull())
            writeMultiCriterionInput(parcel, parents.getOrNull())
            writeStashIDCriterionInput(parcel, stash_id_endpoint.getOrNull())
            writeString(parcel, is_missing.getOrNull())
            writeIntCriterionInput(parcel, rating100.getOrNull())
            writeBoolean(parcel, favorite.getOrNull())
            writeIntCriterionInput(parcel, scene_count.getOrNull())
            writeIntCriterionInput(parcel, image_count.getOrNull())
            writeIntCriterionInput(parcel, gallery_count.getOrNull())
            writeStringCriterionInput(parcel, url.getOrNull())
            writeStringCriterionInput(parcel, aliases.getOrNull())
            writeIntCriterionInput(parcel, child_count.getOrNull())
            writeBoolean(parcel, ignore_auto_tag.getOrNull())
            writeTimestampCriterionInput(parcel, created_at.getOrNull())
            writeTimestampCriterionInput(parcel, updated_at.getOrNull())
        }
    }
}

@Parcelize
@TypeParceler<TagFilterType?, TagFilterTypeParceler>()
data class TagFilterTypeHolder(override val value: TagFilterType?) :
    FilterHolder<TagFilterType>

object TagFilterTypeParceler : Parceler<TagFilterType?> {
    override fun create(parcel: Parcel): TagFilterType? {
        if (parcel.readByte() == ABSENT) {
            return null
        } else {
            return TagFilterType(
                AND = Optional.presentIfNotNull(create(parcel)),
                OR = Optional.presentIfNotNull(create(parcel)),
                NOT = Optional.presentIfNotNull(create(parcel)),
                name = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                aliases = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                favorite = Optional.presentIfNotNull(readBoolean(parcel)),
                description = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                is_missing = Optional.presentIfNotNull(readString(parcel)),
                scene_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                image_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                gallery_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                performer_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                marker_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                parents = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                children = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                parent_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                child_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                ignore_auto_tag = Optional.presentIfNotNull(readBoolean(parcel)),
                created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
            )
        }
    }

    override fun TagFilterType?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        if (this == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)

            AND.getOrNull().write(parcel, flags)
            OR.getOrNull().write(parcel, flags)
            NOT.getOrNull().write(parcel, flags)
            writeStringCriterionInput(parcel, name.getOrNull())
            writeStringCriterionInput(parcel, aliases.getOrNull())
            writeBoolean(parcel, favorite.getOrNull())
            writeStringCriterionInput(parcel, description.getOrNull())
            writeString(parcel, is_missing.getOrNull())
            writeIntCriterionInput(parcel, scene_count.getOrNull())
            writeIntCriterionInput(parcel, image_count.getOrNull())
            writeIntCriterionInput(parcel, gallery_count.getOrNull())
            writeIntCriterionInput(parcel, performer_count.getOrNull())
            writeIntCriterionInput(parcel, marker_count.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, parents.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, children.getOrNull())
            writeIntCriterionInput(parcel, parent_count.getOrNull())
            writeIntCriterionInput(parcel, child_count.getOrNull())
            writeBoolean(parcel, ignore_auto_tag.getOrNull())
            writeTimestampCriterionInput(parcel, created_at.getOrNull())
            writeTimestampCriterionInput(parcel, updated_at.getOrNull())
        }
    }
}

@Parcelize
@TypeParceler<SceneMarkerFilterType?, SceneMarkerFilterTypeParceler>()
data class SceneMarkerFilterTypeHolder(override val value: SceneMarkerFilterType?) :
    FilterHolder<SceneMarkerFilterType>

object SceneMarkerFilterTypeParceler : Parceler<SceneMarkerFilterType?> {
    override fun create(parcel: Parcel): SceneMarkerFilterType? {
        if (parcel.readByte() == ABSENT) {
            return null
        } else {
            return SceneMarkerFilterType(
                tags = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                scene_tags = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                performers = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                scene_date = Optional.presentIfNotNull(readDateCriterionInput(parcel)),
                scene_created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                scene_updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
            )
        }
    }

    override fun SceneMarkerFilterType?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        if (this == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)

            writeHierarchicalMultiCriterionInput(parcel, tags.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, scene_tags.getOrNull())
            writeMultiCriterionInput(parcel, performers.getOrNull())
            writeTimestampCriterionInput(parcel, created_at.getOrNull())
            writeTimestampCriterionInput(parcel, updated_at.getOrNull())
            writeDateCriterionInput(parcel, scene_date.getOrNull())
            writeTimestampCriterionInput(parcel, scene_created_at.getOrNull())
            writeTimestampCriterionInput(parcel, scene_updated_at.getOrNull())
        }
    }
}

@Parcelize
@TypeParceler<ImageFilterType?, ImageFilterTypeParceler>()
data class ImageFilterTypeHolder(override val value: ImageFilterType?) :
    FilterHolder<ImageFilterType>

object ImageFilterTypeParceler : Parceler<ImageFilterType?> {
    override fun create(parcel: Parcel): ImageFilterType? {
        if (parcel.readByte() == ABSENT) {
            return null
        } else {
            return ImageFilterType(
                AND = Optional.presentIfNotNull(create(parcel)),
                OR = Optional.presentIfNotNull(create(parcel)),
                NOT = Optional.presentIfNotNull(create(parcel)),
                title = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                details = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                id = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                checksum = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                path = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                file_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                rating100 = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                date = Optional.presentIfNotNull(readDateCriterionInput(parcel)),
                url = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                organized = Optional.presentIfNotNull(readBoolean(parcel)),
                o_counter = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                resolution = Optional.presentIfNotNull(readResolutionCriterionInput(parcel)),
                orientation = Optional.presentIfNotNull(readOrientationCriterionInput(parcel)),
                is_missing = Optional.presentIfNotNull(readString(parcel)),
                studios = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                tags = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                tag_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                performer_tags =
                    Optional.presentIfNotNull(
                        readHierarchicalMultiCriterionInput(
                            parcel,
                        ),
                    ),
                performers = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                performer_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                performer_favorite = Optional.presentIfNotNull(readBoolean(parcel)),
                performer_age = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                galleries = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                code = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                photographer = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
            )
        }
    }

    override fun ImageFilterType?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        if (this == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)

            AND.getOrNull().write(parcel, flags)
            OR.getOrNull().write(parcel, flags)
            NOT.getOrNull().write(parcel, flags)
            writeStringCriterionInput(parcel, title.getOrNull())
            writeStringCriterionInput(parcel, details.getOrNull())
            writeIntCriterionInput(parcel, id.getOrNull())
            writeStringCriterionInput(parcel, checksum.getOrNull())
            writeStringCriterionInput(parcel, path.getOrNull())
            writeIntCriterionInput(parcel, file_count.getOrNull())
            writeIntCriterionInput(parcel, rating100.getOrNull())
            writeDateCriterionInput(parcel, date.getOrNull())
            writeStringCriterionInput(parcel, url.getOrNull())
            writeBoolean(parcel, organized.getOrNull())
            writeIntCriterionInput(parcel, o_counter.getOrNull())
            writeResolutionCriterionInput(parcel, resolution.getOrNull())
            writeOrientationCriterionInput(parcel, orientation.getOrNull())
            writeString(parcel, is_missing.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, studios.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, tags.getOrNull())
            writeIntCriterionInput(parcel, tag_count.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, performer_tags.getOrNull())
            writeMultiCriterionInput(parcel, performers.getOrNull())
            writeIntCriterionInput(parcel, performer_count.getOrNull())
            writeBoolean(parcel, performer_favorite.getOrNull())
            writeIntCriterionInput(parcel, performer_age.getOrNull())
            writeMultiCriterionInput(parcel, galleries.getOrNull())
            writeTimestampCriterionInput(parcel, created_at.getOrNull())
            writeTimestampCriterionInput(parcel, updated_at.getOrNull())
            writeStringCriterionInput(parcel, code.getOrNull())
            writeStringCriterionInput(parcel, photographer.getOrNull())
        }
    }
}

@Parcelize
@TypeParceler<GalleryFilterType?, GalleryFilterTypeParceler>()
data class GalleryFilterTypeHolder(override val value: GalleryFilterType?) :
    FilterHolder<GalleryFilterType>

object GalleryFilterTypeParceler : Parceler<GalleryFilterType?> {
    override fun create(parcel: Parcel): GalleryFilterType? {
        if (parcel.readByte() == ABSENT) {
            return null
        } else {
            return GalleryFilterType(
                AND = Optional.presentIfNotNull(create(parcel)),
                OR = Optional.presentIfNotNull(create(parcel)),
                NOT = Optional.presentIfNotNull(create(parcel)),
                id = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                title = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                details = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                checksum = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                path = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                file_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                is_missing = Optional.presentIfNotNull(readString(parcel)),
                is_zip = Optional.presentIfNotNull(readBoolean(parcel)),
                rating100 = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                organized = Optional.presentIfNotNull(readBoolean(parcel)),
                average_resolution = Optional.presentIfNotNull(readResolutionCriterionInput(parcel)),
                has_chapters = Optional.presentIfNotNull(readString(parcel)),
                scenes = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                studios = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                tags = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                tag_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                performer_tags =
                    Optional.presentIfNotNull(
                        readHierarchicalMultiCriterionInput(
                            parcel,
                        ),
                    ),
                performers = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                performer_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                performer_favorite = Optional.presentIfNotNull(readBoolean(parcel)),
                performer_age = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                image_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                url = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                date = Optional.presentIfNotNull(readDateCriterionInput(parcel)),
                created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                code = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                photographer = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
            )
        }
    }

    override fun GalleryFilterType?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        if (this == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)

            AND.getOrNull().write(parcel, flags)
            OR.getOrNull().write(parcel, flags)
            NOT.getOrNull().write(parcel, flags)
            writeIntCriterionInput(parcel, id.getOrNull())
            writeStringCriterionInput(parcel, title.getOrNull())
            writeStringCriterionInput(parcel, details.getOrNull())
            writeStringCriterionInput(parcel, checksum.getOrNull())
            writeStringCriterionInput(parcel, path.getOrNull())
            writeIntCriterionInput(parcel, file_count.getOrNull())
            writeString(parcel, is_missing.getOrNull())
            writeBoolean(parcel, is_zip.getOrNull())
            writeIntCriterionInput(parcel, rating100.getOrNull())
            writeBoolean(parcel, organized.getOrNull())
            writeResolutionCriterionInput(parcel, average_resolution.getOrNull())
            writeString(parcel, has_chapters.getOrNull())
            writeMultiCriterionInput(parcel, scenes.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, studios.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, tags.getOrNull())
            writeIntCriterionInput(parcel, tag_count.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, performer_tags.getOrNull())
            writeMultiCriterionInput(parcel, performers.getOrNull())
            writeIntCriterionInput(parcel, performer_count.getOrNull())
            writeBoolean(parcel, performer_favorite.getOrNull())
            writeIntCriterionInput(parcel, performer_age.getOrNull())
            writeIntCriterionInput(parcel, image_count.getOrNull())
            writeStringCriterionInput(parcel, url.getOrNull())
            writeDateCriterionInput(parcel, date.getOrNull())
            writeTimestampCriterionInput(parcel, created_at.getOrNull())
            writeTimestampCriterionInput(parcel, updated_at.getOrNull())
            writeStringCriterionInput(parcel, code.getOrNull())
            writeStringCriterionInput(parcel, photographer.getOrNull())
        }
    }
}

@Parcelize
@TypeParceler<MovieFilterType?, MovieFilterTypeParceler>()
data class MovieFilterTypeHolder(override val value: MovieFilterType?) :
    FilterHolder<MovieFilterType>

object MovieFilterTypeParceler : Parceler<MovieFilterType?> {
    override fun create(parcel: Parcel): MovieFilterType? {
        if (parcel.readByte() == ABSENT) {
            return null
        } else {
            return MovieFilterType(
                name = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                director = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                synopsis = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                duration = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                rating100 = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                studios = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                is_missing = Optional.presentIfNotNull(readString(parcel)),
                url = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                performers = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                date = Optional.presentIfNotNull(readDateCriterionInput(parcel)),
                created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
            )
        }
    }

    override fun MovieFilterType?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        if (this == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)

            writeStringCriterionInput(parcel, name.getOrNull())
            writeStringCriterionInput(parcel, director.getOrNull())
            writeStringCriterionInput(parcel, synopsis.getOrNull())
            writeIntCriterionInput(parcel, duration.getOrNull())
            writeIntCriterionInput(parcel, rating100.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, studios.getOrNull())
            writeString(parcel, is_missing.getOrNull())
            writeStringCriterionInput(parcel, url.getOrNull())
            writeMultiCriterionInput(parcel, performers.getOrNull())
            writeDateCriterionInput(parcel, date.getOrNull())
            writeTimestampCriterionInput(parcel, created_at.getOrNull())
            writeTimestampCriterionInput(parcel, updated_at.getOrNull())
        }
    }
}

fun <T> createFilterHolder(objectFilter: T?): FilterHolder<out Any>? {
    return when (objectFilter) {
        is SceneFilterType -> SceneFilterTypeHolder(objectFilter)
        is PerformerFilterType -> PerformerFilterTypeHolder(objectFilter)
        is StudioFilterType -> StudioFilterTypeHolder(objectFilter)
        is TagFilterType -> TagFilterTypeHolder(objectFilter)
        is SceneMarkerFilterType -> SceneMarkerFilterTypeHolder(objectFilter)
        is ImageFilterType -> ImageFilterTypeHolder(objectFilter)
        is GalleryFilterType -> GalleryFilterTypeHolder(objectFilter)
        is MovieFilterType -> MovieFilterTypeHolder(objectFilter)
        null -> null
        else -> throw IllegalArgumentException("Unknown filter type: ${objectFilter!!::class.java}")
    }
}
