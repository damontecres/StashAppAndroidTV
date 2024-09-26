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
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MovieFilterType
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
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput

/**
 * Parse a server-side filter from JSON (Map<String, *>)
 */
class FilterParser(private val serverVersion: Version) {
    companion object {
        const val TAG = "FilterParser"
    }

    private fun convertIntCriterionInput(it: Map<String, *>?): IntCriterionInput? {
        return if (it != null) {
            val values = it["value"] as Map<String, *>?
            IntCriterionInput(
                value = values?.get("value")?.toString()?.toInt() ?: 0,
                value2 = Optional.presentIfNotNull(values?.get("value2")?.toString()?.toInt()),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertFloatCriterionInput(it: Map<String, *>?): FloatCriterionInput? {
        return if (it != null) {
            val values = it["value"] as Map<String, *>? // Might be an int or double
            FloatCriterionInput(
                value = values?.get("value")?.toString()?.toDouble() ?: 0.0,
                value2 = Optional.presentIfNotNull(values?.get("value2")?.toString()?.toDouble()),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertStringCriterionInput(it: Map<String, *>?): StringCriterionInput? {
        return if (it != null) {
            StringCriterionInput(
                value = it["value"]?.toString() ?: "",
                modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
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
                value = Optional.presentIfNotNull(items),
                modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                depth = Optional.presentIfNotNull(values["depth"] as Int?),
                excludes = Optional.presentIfNotNull(excludes),
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
                    value = Optional.presentIfNotNull(items),
                    modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                    excludes = Optional.presentIfNotNull(excludes),
                )
            } else if (it["value"] != null && it["value"] is List<*>) {
                val items = (it["value"] as List<*>).map { it.toString() }
                MultiCriterionInput(
                    value = Optional.presentIfNotNull(items),
                    modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                    excludes = Optional.absent(),
                )
            } else {
                MultiCriterionInput(
                    value = Optional.absent(),
                    modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                    excludes = Optional.absent(),
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
            val values = it["value"] as Map<String, String?>?
            DateCriterionInput(
                value = values?.get("value") ?: "",
                value2 = Optional.presentIfNotNull(values?.get("value2")),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertTimestampCriterionInput(it: Map<String, *>?): TimestampCriterionInput? {
        return if (it != null) {
            val values = it["value"] as Map<String, String?>?
            TimestampCriterionInput(
                value = values?.get("value") ?: "",
                value2 = Optional.presentIfNotNull(values?.get("value2")),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertCircumcisionCriterionInput(it: Map<String, *>?): CircumcisionCriterionInput? {
        return if (it != null) {
            val valueList = (it["value"] as List<String>?)
            CircumcisionCriterionInput(
                value =
                    Optional.presentIfNotNull(
                        valueList?.map { CircumisedEnum.valueOf(it.uppercase()) }
                            ?.toList(),
                    ),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
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
            values =
                values.map {
                    it.uppercase()
                        .replace(" ", "_")
                        .replace("-", "_")
                }

            GenderCriterionInput(
                value = Optional.absent(),
                value_list = Optional.present(values.map(GenderEnum::valueOf)),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
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
                endpoint = Optional.presentIfNotNull(values?.get("endpoint")),
                stash_id = Optional.presentIfNotNull(values?.get("stashID")),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertPhashDistanceCriterionInput(it: Map<String, *>?): PhashDistanceCriterionInput? {
        return if (it != null) {
            val values = it["value"] as Map<String, *>?
            PhashDistanceCriterionInput(
                value = values?.get("value") as String? ?: "",
                modifier = CriterionModifier.valueOf(it["modifier"]!!.toString()),
                distance = Optional.presentIfNotNull(values?.get("distance")?.toString()?.toInt()),
            )
        } else {
            null
        }
    }

    private fun convertPHashDuplicationCriterionInput(it: Map<String, *>?): PHashDuplicationCriterionInput? {
        return if (it != null) {
            PHashDuplicationCriterionInput(
                duplicated = Optional.presentIfNotNull(it["duplicated"]?.toString()?.toBoolean()),
                distance = Optional.presentIfNotNull(it["distance"]?.toString()?.toInt()),
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
                value = convertToResolutionEnum(it["value"].toString()),
                modifier = CriterionModifier.valueOf(it["modifier"]!! as String),
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

    fun convertPerformerObjectFilter(f: Any?): PerformerFilterType? {
        return if (f != null && f is PerformerFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            PerformerFilterType(
                AND = Optional.presentIfNotNull(convertPerformerObjectFilter(filter["AND"])),
                OR = Optional.presentIfNotNull(convertPerformerObjectFilter(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertPerformerObjectFilter(filter["NOT"])),
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
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
            )
        } else {
            null
        }
    }

    fun convertSceneObjectFilter(f: Any?): SceneFilterType? {
        return if (f != null && f is SceneFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            SceneFilterType(
                AND = Optional.presentIfNotNull(convertSceneObjectFilter(filter["AND"])),
                OR = Optional.presentIfNotNull(convertSceneObjectFilter(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertSceneObjectFilter(filter["NOT"])),
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
                movies = Optional.presentIfNotNull(convertMultiCriterionInput(filter["movies"])),
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
            )
        } else {
            null
        }
    }

    fun convertStudioObjectFilter(f: Any?): StudioFilterType? {
        return if (f != null && f is StudioFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            StudioFilterType(
                AND = Optional.presentIfNotNull(convertStudioObjectFilter(filter["AND"])),
                OR = Optional.presentIfNotNull(convertStudioObjectFilter(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertStudioObjectFilter(filter["NOT"])),
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter["name"])),
                details = Optional.presentIfNotNull(convertStringCriterionInput(filter["details"])),
                parents = Optional.presentIfNotNull(convertMultiCriterionInput(filter["parents"])),
                stash_id_endpoint =
                    Optional.presentIfNotNull(
                        convertStashIDCriterionInput(
                            filter["stash_id_endpoint"],
                        ),
                    ),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter["rating100"])),
                favorite = Optional.presentIfNotNull(convertBoolean(filter["favorite"])),
                scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["scene_count"])),
                image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["image_count"])),
                gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["gallery_count"])),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter["url"])),
                aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter["aliases"])),
                child_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["child_count"])),
                ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter["ignore_auto_tag"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
            )
        } else {
            null
        }
    }

    fun convertTagObjectFilter(f: Any?): TagFilterType? {
        return if (f != null && f is TagFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            TagFilterType(
                AND = Optional.presentIfNotNull(convertTagObjectFilter(filter["AND"])),
                OR = Optional.presentIfNotNull(convertTagObjectFilter(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertTagObjectFilter(filter["NOT"])),
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter["name"])),
                aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter["aliases"])),
                favorite = Optional.presentIfNotNull(convertBoolean(filter["favorite"])),
                description = Optional.presentIfNotNull(convertStringCriterionInput(filter["description"])),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["scene_count"])),
                image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["image_count"])),
                gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["gallery_count"])),
                performer_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["performer_count"])),
                marker_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["marker_count"])),
                parents = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["parents"])),
                children = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["children"])),
                parent_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["parent_count"])),
                child_count = Optional.presentIfNotNull(convertIntCriterionInput(filter["child_count"])),
                ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter["ignore_auto_tag"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
            )
        } else {
            null
        }
    }

    fun convertMovieObjectFilter(f: Any?): MovieFilterType? {
        return if (f != null && f is MovieFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            MovieFilterType(
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter["name"])),
                director = Optional.presentIfNotNull(convertStringCriterionInput(filter["director"])),
                synopsis = Optional.presentIfNotNull(convertStringCriterionInput(filter["synopsis"])),
                duration = Optional.presentIfNotNull(convertIntCriterionInput(filter["duration"])),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter["rating100"])),
                studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["studios"])),
                is_missing = Optional.presentIfNotNull(convertString(filter["is_missing"])),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter["url"])),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter["performers"])),
                date = Optional.presentIfNotNull(convertDateCriterionInput(filter["date"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
            )
        } else {
            null
        }
    }

    fun convertMarkerObjectFilter(f: Any?): SceneMarkerFilterType? {
        return if (f != null && f is SceneMarkerFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            SceneMarkerFilterType(
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["tags"])),
                scene_tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter["scene_tags"])),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter["performers"])),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["created_at"])),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["updated_at"])),
                scene_date = Optional.presentIfNotNull(convertDateCriterionInput(filter["scene_date"])),
                scene_created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["scene_created_at"])),
                scene_updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter["scene_updated_at"])),
            )
        } else {
            null
        }
    }

    fun convertImageObjectFilter(f: Any?): ImageFilterType? {
        return if (f != null && f is ImageFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            ImageFilterType(
                AND = Optional.presentIfNotNull(convertImageObjectFilter(filter["AND"])),
                OR = Optional.presentIfNotNull(convertImageObjectFilter(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertImageObjectFilter(filter["NOT"])),
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
            )
        } else {
            null
        }
    }

    fun convertGalleryObjectFilter(f: Any?): GalleryFilterType? {
        return if (f != null && f is GalleryFilterType) {
            f
        } else if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            GalleryFilterType(
                AND = Optional.presentIfNotNull(convertGalleryObjectFilter(filter["AND"])),
                OR = Optional.presentIfNotNull(convertGalleryObjectFilter(filter["OR"])),
                NOT = Optional.presentIfNotNull(convertGalleryObjectFilter(filter["NOT"])),
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
            )
        } else {
            null
        }
    }
}
