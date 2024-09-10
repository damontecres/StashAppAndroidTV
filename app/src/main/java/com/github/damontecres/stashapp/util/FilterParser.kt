package com.github.damontecres.stashapp.util

import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CircumcisionCriterionInput
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GenderCriterionInput
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationEnum
import com.github.damontecres.stashapp.api.type.PHashDuplicationCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.PhashDistanceCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionEnum
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter

class FilterParser(private val serverVersion: Version) {
    companion object {
        const val TAG = "FilterParser"
    }

    private fun convertIntCriterionInput(it: Map<String, *>?): IntCriterionInput? {
        return if (it != null) {
            val values = it["value"]!! as Map<String, *>
            IntCriterionInput(
                values["value"]?.toString()?.toInt() ?: 0,
                Optional.presentIfNotNull(values["value2"]?.toString()?.toInt()),
                CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertFloatCriterionInput(it: Map<String, *>?): FloatCriterionInput? {
        return if (it != null) {
            val values = it["value"]!! as Map<String, *> // Might be an int or double
            FloatCriterionInput(
                values["value"]?.toString()?.toDouble() ?: 0.0,
                Optional.presentIfNotNull(values["value2"]?.toString()?.toDouble()),
                CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertStringCriterionInput(it: Map<String, *>?): StringCriterionInput? {
        return if (it != null) {
            StringCriterionInput(
                it["value"]?.toString() ?: "",
                CriterionModifier.valueOf(it["modifier"]!!.toString()),
            )
        } else {
            null
        }
    }

    private fun mapToIds(list: Any?): List<String>? {
        return if (list != null && list is List<*>) {
            list.mapNotNull { (it as Map<*, *>)["id"]?.toString() }.toList()
        } else {
            null
        }
    }

    private fun convertHierarchicalMultiCriterionInput(it: Map<String, *>?): HierarchicalMultiCriterionInput? {
        return if (it != null) {
            val values = it["value"]!! as Map<String, *>
            val items = mapToIds(values["items"])
            val excludes = mapToIds(values["excluded"])
            HierarchicalMultiCriterionInput(
                Optional.presentIfNotNull(items),
                CriterionModifier.valueOf(it["modifier"]!!.toString()),
                Optional.presentIfNotNull(values["depth"] as Int?),
                Optional.presentIfNotNull(excludes),
            )
        } else {
            null
        }
    }

    private fun convertMultiCriterionInput(it: Map<String, *>?): MultiCriterionInput? {
        return if (it != null) {
            if (it["value"] != null && it["value"] is Map<*, *>) {
                val values = it["value"] as Map<String, *>
                val items = mapToIds(values["items"])
                val excludes = mapToIds(values["excluded"])
                MultiCriterionInput(
                    Optional.presentIfNotNull(items),
                    CriterionModifier.valueOf(it["modifier"]!!.toString()),
                    Optional.presentIfNotNull(excludes),
                )
            } else if (it["value"] != null && it["value"] is List<*>) {
                val items = (it["value"] as List<*>).map { it.toString() }
                MultiCriterionInput(
                    Optional.presentIfNotNull(items),
                    CriterionModifier.valueOf(it["modifier"]!!.toString()),
                    Optional.absent(),
                )
            } else {
                MultiCriterionInput(
                    Optional.absent(),
                    CriterionModifier.valueOf(it["modifier"]!!.toString()),
                    Optional.absent(),
                )
            }
        } else {
            null
        }
    }

    private fun convertBoolean(it: Map<String, *>?): Boolean? {
        return if (it != null) {
            val value = (it["value"] as String?).toBoolean()
            when (CriterionModifier.valueOf(it["modifier"] as String)) {
                CriterionModifier.EQUALS -> value
                CriterionModifier.NOT_EQUALS -> !value
                else -> null
            }
        } else {
            null
        }
    }

    private fun convertDateCriterionInput(it: Map<String, *>?): DateCriterionInput? {
        return if (it != null) {
            val values = it["value"]!! as Map<String, String?>
            DateCriterionInput(
                values["value"]!!,
                Optional.presentIfNotNull(values["value2"]),
                CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertTimestampCriterionInput(it: Map<String, *>?): TimestampCriterionInput? {
        return if (it != null) {
            val values = it["value"]!! as Map<String, String?>
            TimestampCriterionInput(
                values["value"]!!,
                Optional.presentIfNotNull(values["value2"]),
                CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertCircumcisionCriterionInput(it: Map<String, *>?): CircumcisionCriterionInput? {
        return if (it != null) {
            val valueList = (it["value"] as List<String>?)
            CircumcisionCriterionInput(
                Optional.presentIfNotNull(
                    valueList?.map { CircumisedEnum.valueOf(it.uppercase()) }
                        ?.toList(),
                ),
                CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertGenderCriterionInput(it: Map<String, *>?): GenderCriterionInput? {
        return if (it != null) {
            val value = it["value"]
            var values =
                if (value is List<*>) {
                    val v = value.filterNotNull().map { it.toString() }
                    v
                } else if (value != null) {
                    val v = listOf(value.toString())
                    v
                } else {
                    val v = emptyList<String>()
                    v
                }
            values = values.map { it.uppercase().replace(" ", "_") }

            GenderCriterionInput(
                Optional.absent(),
                Optional.present(values.map(GenderEnum::valueOf)),
                CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertString(it: Map<String, *>?): String? {
        return if (it != null) {
            return it["value"]?.toString()
        } else {
            null
        }
    }

    private fun convertStashIDCriterionInput(it: Map<String, *>?): StashIDCriterionInput? {
        return if (it != null) {
            val values = it["value"] as Map<String, String?>?
            StashIDCriterionInput(
                Optional.presentIfNotNull(values?.get("endpoint")),
                Optional.presentIfNotNull(values?.get("stashID")),
                CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertPhashDistanceCriterionInput(it: Map<String, *>?): PhashDistanceCriterionInput? {
        return if (it != null) {
            val values = it["value"]!! as Map<String, *>
            PhashDistanceCriterionInput(
                values["value"]!! as String,
                CriterionModifier.valueOf(it["modifier"]!!.toString()),
                Optional.presentIfNotNull(values["distance"]?.toString()?.toInt()),
            )
        } else {
            null
        }
    }

    private fun convertPHashDuplicationCriterionInput(it: Map<String, *>?): PHashDuplicationCriterionInput? {
        return if (it != null) {
            PHashDuplicationCriterionInput(
                Optional.presentIfNotNull(it["duplicated"]?.toString()?.toBoolean()),
                Optional.presentIfNotNull(it["distance"]?.toString()?.toInt()),
            )
        } else {
            null
        }
    }

    private fun convertToResolutionEnum(str: String): ResolutionEnum {
        return when (str) {
            "114p" -> ResolutionEnum.VERY_LOW
            "240p" -> ResolutionEnum.LOW
            "360p" -> ResolutionEnum.R360P
            "480p" -> ResolutionEnum.STANDARD
            "540p" -> ResolutionEnum.WEB_HD
            "720p" -> ResolutionEnum.STANDARD_HD
            "1080p" -> ResolutionEnum.FULL_HD
            "1440p" -> ResolutionEnum.QUAD_HD
            "4k" -> ResolutionEnum.FOUR_K
            "5k" -> ResolutionEnum.FIVE_K
            "6k" -> ResolutionEnum.SIX_K
            "7k" -> ResolutionEnum.SEVEN_K
            "8k" -> ResolutionEnum.EIGHT_K
            "Huge" -> ResolutionEnum.HUGE
            else -> ResolutionEnum.UNKNOWN__
        }
    }

    private fun convertResolutionCriterionInput(it: Map<String, *>?): ResolutionCriterionInput? {
        return if (it != null) {
            ResolutionCriterionInput(
                convertToResolutionEnum(it["value"].toString()),
                CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertOrientationCriterionInput(it: Map<String, *>?): OrientationCriterionInput? {
        return if (it != null) {
            val value = it["value"]
            if (value is List<*>) {
                OrientationCriterionInput(
                    value.mapNotNull {
                        OrientationEnum.valueOf(
                            it.toString().uppercase(),
                        )
                    },
                )
            } else {
                OrientationCriterionInput(
                    listOf(
                        OrientationEnum.valueOf(
                            value.toString().uppercase(),
                        ),
                    ),
                )
            }
        } else {
            null
        }
    }

    fun convertFindFilter(f: Any?): StashFindFilter? {
        return if (f is StashFindFilter) {
            f
        } else if (f == null) {
            null
        } else {
            val filter = f as Map<String, String>
            val sort = filter.getCaseInsensitive("sort")
            val direction =
                SortDirectionEnum.entries.find { it.name == (filter.getCaseInsensitive("direction")) }
                    ?: SortDirectionEnum.ASC
            val sortAndDirection =
                if (sort.isNotNullOrBlank()) {
                    SortAndDirection(sort, direction)
                } else {
                    null
                }
            StashFindFilter(filter.getCaseInsensitive("q"), sortAndDirection)
        }
    }

    fun convertFilter(
        dataType: DataType,
        f: Any?,
    ): StashDataFilter? {
        return when (dataType) {
            DataType.TAG -> convertTagFilterType(f)
            DataType.STUDIO -> convertStudioFilterType(f)
            DataType.GROUP -> convertGroupFilterType(f)
            DataType.SCENE -> convertSceneFilterType(f)
            DataType.IMAGE -> convertImageFilterType(f)
            DataType.GALLERY -> convertGalleryFilterType(f)
            DataType.MARKER -> convertSceneMarkerFilterType(f)
            DataType.PERFORMER -> convertPerformerFilterType(f)
        }
    }

    fun convertPerformerFilterType(f: Any?): PerformerFilterType? {
        return if (f != null && f is PerformerFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            PerformerFilterType(
                AND = Optional.presentIfNotNull(convertPerformerFilterType(filter["AND"])),
                OR = Optional.presentIfNotNull(convertPerformerFilterType(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertPerformerFilterType(filter["NOT"])),
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter["name"])),
                disambiguation = Optional.presentIfNotNull(convertStringCriterionInput(filter["disambiguation"])),
                details = Optional.presentIfNotNull(convertStringCriterionInput(filter["details"])),
                filter_favorites = Optional.presentIfNotNull(convertBoolean(filter["filter_favorites"])),
                birth_year = Optional.presentIfNotNull(convertIntCriterionInput(filter["birth_year"])),
                age = Optional.presentIfNotNull(convertIntCriterionInput(filter["age"])),
                ethnicity = Optional.presentIfNotNull(convertStringCriterionInput(filter["ethnicity"])),
                country = Optional.presentIfNotNull(convertStringCriterionInput(filter["country"])),
                eye_color = Optional.presentIfNotNull(convertStringCriterionInput(filter["eye_color"])),
                height_cm = Optional.presentIfNotNull(convertIntCriterionInput(filter["height_cm"])),
                measurements = Optional.presentIfNotNull(convertStringCriterionInput(filter["measurements"])),
                fake_tits = Optional.presentIfNotNull(convertStringCriterionInput(filter["fake_tits"])),
                penis_length = Optional.presentIfNotNull(convertFloatCriterionInput(filter["penis_length"])),
                circumcised = Optional.presentIfNotNull(convertCircumcisionCriterionInput(filter["circumcised"])),
                career_length = Optional.presentIfNotNull(convertStringCriterionInput(filter["career_length"])),
                tattoos = Optional.presentIfNotNull(convertStringCriterionInput(filter["tattoos"])),
                piercings = Optional.presentIfNotNull(convertStringCriterionInput(filter["piercings"])),
                aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter["aliases"])),
                gender = Optional.presentIfNotNull(convertGenderCriterionInput(filter["gender"])),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["tags"])),
                tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["tag_count"])),
                scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["scene_count"])),
                image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["image_count"])),
                gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["gallery_count"])),
                play_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["play_count"])),
                o_counter = Optional.presentIfNotNull(convertIntCriterionInput(filter["o_counter"])),
                stash_id_endpoint = Optional.presentIfNotNull(convertStashIDCriterionInput(filter["stash_id_endpoint"])),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter["rating100"])),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter["url"])),
                hair_color = Optional.presentIfNotNull(convertStringCriterionInput(filter["hair_color"])),
                weight = Optional.presentIfNotNull(convertIntCriterionInput(filter["weight"])),
                death_year = Optional.presentIfNotNull(convertIntCriterionInput(filter["death_year"])),
                studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["studios"])),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter["performers"])),
                ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter["ignore_auto_tag"])),
                birthdate = Optional.presentIfNotNull(convertDateCriterionInput(filter["birthdate"])),
                death_date = Optional.presentIfNotNull(convertDateCriterionInput(filter["death_date"])),
                scenes_filter = Optional.presentIfNotNull(convertSceneFilterType(filter["scenes_filter"])),
                images_filter = Optional.presentIfNotNull(convertImageFilterType(filter["images_filter"])),
                galleries_filter = Optional.presentIfNotNull(convertGalleryFilterType(filter["galleries_filter"])),
                tags_filter = Optional.presentIfNotNull(convertTagFilterType(filter["tags_filter"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
            )
        } else {
            null
        }
    }

    fun convertSceneFilterType(f: Any?): SceneFilterType? {
        return if (f != null && f is SceneFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            SceneFilterType(
                AND = Optional.presentIfNotNull(convertSceneFilterType(filter["AND"])),
                OR = Optional.presentIfNotNull(convertSceneFilterType(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertSceneFilterType(filter["NOT"])),
                id = Optional.presentIfNotNull(convertIntCriterionInput(filter["id"])),
                title = Optional.presentIfNotNull(convertStringCriterionInput(filter["title"])),
                code = Optional.presentIfNotNull(convertStringCriterionInput(filter["code"])),
                details = Optional.presentIfNotNull(convertStringCriterionInput(filter["details"])),
                director = Optional.presentIfNotNull(convertStringCriterionInput(filter["director"])),
                oshash = Optional.presentIfNotNull(convertStringCriterionInput(filter["oshash"])),
                checksum = Optional.presentIfNotNull(convertStringCriterionInput(filter["checksum"])),
                phash = Optional.presentIfNotNull(convertStringCriterionInput(filter["phash"])),
                phash_distance = Optional.presentIfNotNull(convertPhashDistanceCriterionInput(filter["phash_distance"])),
                path = Optional.presentIfNotNull(convertStringCriterionInput(filter["path"])),
                file_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["file_count"])),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter["rating100"])),
                organized = Optional.presentIfNotNull(convertBoolean(filter["organized"])),
                o_counter = Optional.presentIfNotNull(convertIntCriterionInput(filter["o_counter"])),
                duplicated = Optional.presentIfNotNull(convertPHashDuplicationCriterionInput(filter["duplicated"])),
                resolution = Optional.presentIfNotNull(convertResolutionCriterionInput(filter["resolution"])),
                orientation = Optional.presentIfNotNull(convertOrientationCriterionInput(filter["orientation"])),
                framerate = Optional.presentIfNotNull(convertIntCriterionInput(filter["framerate"])),
                bitrate = Optional.presentIfNotNull(convertIntCriterionInput(filter["bitrate"])),
                video_codec = Optional.presentIfNotNull(convertStringCriterionInput(filter["video_codec"])),
                audio_codec = Optional.presentIfNotNull(convertStringCriterionInput(filter["audio_codec"])),
                duration = Optional.presentIfNotNull(convertIntCriterionInput(filter["duration"])),
                has_markers = Optional.presentIfNotNull(convertString(filter["has_markers"])),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["studios"])),
                groups = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["groups"])),
                galleries = Optional.presentIfNotNull(convertMultiCriterionInput(filter["galleries"])),
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["tags"])),
                tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["tag_count"])),
                performer_tags =
                    Optional.presentIfNotNull(
                        convertHierarchicalMultiCriterionInput(
                            filter["performer_tags"],
                        ),
                    ),
                performer_favorite = Optional.presentIfNotNull(convertBoolean(filter["performer_favorite"])),
                performer_age = Optional.presentIfNotNull(convertIntCriterionInput(filter["performer_age"])),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter["performers"])),
                performer_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["performer_count"])),
                stash_id_endpoint = Optional.presentIfNotNull(convertStashIDCriterionInput(filter["stash_id_endpoint"])),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter["url"])),
                interactive = Optional.presentIfNotNull(convertBoolean(filter["interactive"])),
                interactive_speed = Optional.presentIfNotNull(convertIntCriterionInput(filter["interactive_speed"])),
                captions = Optional.presentIfNotNull(convertStringCriterionInput(filter["captions"])),
                resume_time = Optional.presentIfNotNull(convertIntCriterionInput(filter["resume_time"])),
                play_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["play_count"])),
                play_duration = Optional.presentIfNotNull(convertIntCriterionInput(filter["play_duration"])),
                last_played_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["last_played_at"])),
                date = Optional.presentIfNotNull(convertDateCriterionInput(filter["date"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
                galleries_filter = Optional.presentIfNotNull(convertGalleryFilterType(filter["galleries_filter"])),
                performers_filter = Optional.presentIfNotNull(convertPerformerFilterType(filter["performers_filter"])),
                studios_filter = Optional.presentIfNotNull(convertStudioFilterType(filter["studios_filter"])),
                tags_filter = Optional.presentIfNotNull(convertTagFilterType(filter["tags_filter"])),
                groups_filter = Optional.presentIfNotNull(convertGroupFilterType(filter["groups_filter"])),
                // TODO group->group
//                groups_filter = Optional.presentIfNotNull(convertGroupFilterType(filter["groups_filter"])),
                markers_filter = Optional.presentIfNotNull(convertSceneMarkerFilterType(filter["markers_filter"])),
            )
        } else {
            null
        }
    }

    fun convertStudioFilterType(f: Any?): StudioFilterType? {
        return if (f != null && f is StudioFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            StudioFilterType(
                AND = Optional.presentIfNotNull(convertStudioFilterType(filter["AND"])),
                OR = Optional.presentIfNotNull(convertStudioFilterType(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertStudioFilterType(filter["NOT"])),
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter["name"])),
                details = Optional.presentIfNotNull(convertStringCriterionInput(filter["details"])),
                parents = Optional.presentIfNotNull(convertMultiCriterionInput(filter["parents"])),
                stash_id_endpoint = Optional.presentIfNotNull(convertStashIDCriterionInput(filter["stash_id_endpoint"])),
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["tags"])),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter["rating100"])),
                favorite = Optional.presentIfNotNull(convertBoolean(filter["favorite"])),
                scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["scene_count"])),
                image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["image_count"])),
                gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["gallery_count"])),
                tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["tag_count"])),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter["url"])),
                aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter["aliases"])),
                child_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["child_count"])),
                ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter["ignore_auto_tag"])),
                scenes_filter = Optional.presentIfNotNull(convertSceneFilterType(filter["scenes_filter"])),
                images_filter = Optional.presentIfNotNull(convertImageFilterType(filter["images_filter"])),
                galleries_filter = Optional.presentIfNotNull(convertGalleryFilterType(filter["galleries_filter"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
            )
        } else {
            null
        }
    }

    fun convertTagFilterType(f: Any?): TagFilterType? {
        return if (f != null && f is TagFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            TagFilterType(
                AND = Optional.presentIfNotNull(convertTagFilterType(filter["AND"])),
                OR = Optional.presentIfNotNull(convertTagFilterType(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertTagFilterType(filter["NOT"])),
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter["name"])),
                aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter["aliases"])),
                favorite = Optional.presentIfNotNull(convertBoolean(filter["favorite"])),
                description = Optional.presentIfNotNull(convertStringCriterionInput(filter["description"])),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["scene_count"])),
                image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["image_count"])),
                gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["gallery_count"])),
                performer_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["performer_count"])),
                studio_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["studio_count"])),
                group_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["group_count"])),
                marker_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["marker_count"])),
                parents = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["parents"])),
                children = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["children"])),
                parent_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["parent_count"])),
                child_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["child_count"])),
                ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter["ignore_auto_tag"])),
                scenes_filter = Optional.presentIfNotNull(convertSceneFilterType(filter["scenes_filter"])),
                images_filter = Optional.presentIfNotNull(convertImageFilterType(filter["images_filter"])),
                galleries_filter = Optional.presentIfNotNull(convertGalleryFilterType(filter["galleries_filter"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
            )
        } else {
            null
        }
    }

    fun convertGroupFilterType(f: Any?): GroupFilterType? {
        return if (f != null && f is GroupFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            GroupFilterType(
                AND = Optional.presentIfNotNull(convertGroupFilterType(filter["AND"])),
                OR = Optional.presentIfNotNull(convertGroupFilterType(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertGroupFilterType(filter["NOT"])),
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter["name"])),
                director = Optional.presentIfNotNull(convertStringCriterionInput(filter["director"])),
                synopsis = Optional.presentIfNotNull(convertStringCriterionInput(filter["synopsis"])),
                duration = Optional.presentIfNotNull(convertIntCriterionInput(filter["duration"])),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter["rating100"])),
                studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["studios"])),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter["url"])),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter["performers"])),
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["tags"])),
                tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["tag_count"])),
                date = Optional.presentIfNotNull(convertDateCriterionInput(filter["date"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
                scenes_filter = Optional.presentIfNotNull(convertSceneFilterType(filter["scenes_filter"])),
                studios_filter = Optional.presentIfNotNull(convertStudioFilterType(filter["studios_filter"])),
            )
        } else {
            null
        }
    }

    fun convertSceneMarkerFilterType(f: Any?): SceneMarkerFilterType? {
        return if (f != null && f is SceneMarkerFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            SceneMarkerFilterType(
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["tags"])),
                scene_tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["scene_tags"])),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter["performers"])),
                scenes = Optional.presentIfNotNull(convertMultiCriterionInput(filter["scenes"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
                scene_date = Optional.presentIfNotNull(convertDateCriterionInput(filter["scene_date"])),
                scene_created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["scene_created_at"])),
                scene_updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["scene_updated_at"])),
                scene_filter = Optional.presentIfNotNull(convertSceneFilterType(filter["scene_filter"])),
            )
        } else {
            null
        }
    }

    fun convertImageFilterType(f: Any?): ImageFilterType? {
        return if (f != null && f is ImageFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            ImageFilterType(
                AND = Optional.presentIfNotNull(convertImageFilterType(filter["AND"])),
                OR = Optional.presentIfNotNull(convertImageFilterType(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertImageFilterType(filter["NOT"])),
                title = Optional.presentIfNotNull(convertStringCriterionInput(filter["title"])),
                details = Optional.presentIfNotNull(convertStringCriterionInput(filter["details"])),
                id = Optional.presentIfNotNull(convertIntCriterionInput(filter["id"])),
                checksum = Optional.presentIfNotNull(convertStringCriterionInput(filter["checksum"])),
                path = Optional.presentIfNotNull(convertStringCriterionInput(filter["path"])),
                file_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["file_count"])),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter["rating100"])),
                date = Optional.presentIfNotNull(convertDateCriterionInput(filter["date"])),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter["url"])),
                organized = Optional.presentIfNotNull(convertBoolean(filter["organized"])),
                o_counter = Optional.presentIfNotNull(convertIntCriterionInput(filter["o_counter"])),
                resolution = Optional.presentIfNotNull(convertResolutionCriterionInput(filter["resolution"])),
                orientation = Optional.presentIfNotNull(convertOrientationCriterionInput(filter["orientation"])),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["studios"])),
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["tags"])),
                tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["tag_count"])),
                performer_tags =
                    Optional.presentIfNotNull(
                        convertHierarchicalMultiCriterionInput(
                            filter["performer_tags"],
                        ),
                    ),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter["performers"])),
                performer_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["performer_count"])),
                performer_favorite = Optional.presentIfNotNull(convertBoolean(filter["performer_favorite"])),
                performer_age = Optional.presentIfNotNull(convertIntCriterionInput(filter["performer_age"])),
                galleries = Optional.presentIfNotNull(convertMultiCriterionInput(filter["galleries"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
                code = Optional.presentIfNotNull(convertStringCriterionInput(filter["code"])),
                photographer = Optional.presentIfNotNull(convertStringCriterionInput(filter["photographer"])),
                galleries_filter = Optional.presentIfNotNull(convertGalleryFilterType(filter["galleries_filter"])),
                performers_filter = Optional.presentIfNotNull(convertPerformerFilterType(filter["performers_filter"])),
                studios_filter = Optional.presentIfNotNull(convertStudioFilterType(filter["studios_filter"])),
                tags_filter = Optional.presentIfNotNull(convertTagFilterType(filter["tags_filter"])),
            )
        } else {
            null
        }
    }

    fun convertGalleryFilterType(f: Any?): GalleryFilterType? {
        return if (f != null && f is GalleryFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            GalleryFilterType(
                AND = Optional.presentIfNotNull(convertGalleryFilterType(filter["AND"])),
                OR = Optional.presentIfNotNull(convertGalleryFilterType(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertGalleryFilterType(filter["NOT"])),
                id = Optional.presentIfNotNull(convertIntCriterionInput(filter["id"])),
                title = Optional.presentIfNotNull(convertStringCriterionInput(filter["title"])),
                details = Optional.presentIfNotNull(convertStringCriterionInput(filter["details"])),
                checksum = Optional.presentIfNotNull(convertStringCriterionInput(filter["checksum"])),
                path = Optional.presentIfNotNull(convertStringCriterionInput(filter["path"])),
                file_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["file_count"])),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                is_zip = Optional.presentIfNotNull(convertBoolean(filter["is_zip"])),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter["rating100"])),
                organized = Optional.presentIfNotNull(convertBoolean(filter["organized"])),
                average_resolution =
                    Optional.presentIfNotNull(
                        convertResolutionCriterionInput(
                            filter["average_resolution"],
                        ),
                    ),
                has_chapters = Optional.presentIfNotNull(convertString(filter["has_chapters"])),
                scenes = Optional.presentIfNotNull(convertMultiCriterionInput(filter["scenes"])),
                studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["studios"])),
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["tags"])),
                tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["tag_count"])),
                performer_tags =
                    Optional.presentIfNotNull(
                        convertHierarchicalMultiCriterionInput(
                            filter["performer_tags"],
                        ),
                    ),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter["performers"])),
                performer_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["performer_count"])),
                performer_favorite = Optional.presentIfNotNull(convertBoolean(filter["performer_favorite"])),
                performer_age = Optional.presentIfNotNull(convertIntCriterionInput(filter["performer_age"])),
                image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["image_count"])),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter["url"])),
                date = Optional.presentIfNotNull(convertDateCriterionInput(filter["date"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
                code = Optional.presentIfNotNull(convertStringCriterionInput(filter["code"])),
                photographer = Optional.presentIfNotNull(convertStringCriterionInput(filter["photographer"])),
                scenes_filter = Optional.presentIfNotNull(convertSceneFilterType(filter["scenes_filter"])),
                images_filter = Optional.presentIfNotNull(convertImageFilterType(filter["images_filter"])),
                performers_filter = Optional.presentIfNotNull(convertPerformerFilterType(filter["performers_filter"])),
                studios_filter = Optional.presentIfNotNull(convertStudioFilterType(filter["studios_filter"])),
                tags_filter = Optional.presentIfNotNull(convertTagFilterType(filter["tags_filter"])),
            )
        } else {
            null
        }
    }
}
