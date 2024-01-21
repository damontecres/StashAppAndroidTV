package com.github.damontecres.stashapp

import android.content.Context
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.type.CircumcisionCriterionInput
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.GenderCriterionInput
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PHashDuplicationCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.PhashDistanceCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionEnum
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput

class FilterParser private constructor(context: Context, serverInfoQuery: ServerInfoQuery.Data?) {
    private val serverPreferences = ServerPreferences(context)
    private val serverVersion =
        if (serverInfoQuery?.version?.version != null) Version(serverInfoQuery.version.version) else null

    companion object {
        lateinit var instance: FilterParser

        fun initialize(
            context: Context,
            serverInfoQuery: ServerInfoQuery.Data?,
        ) {
            instance = FilterParser(context, serverInfoQuery)
        }
    }

    private fun convertIntCriterionInput(it: Map<String, *>?): IntCriterionInput? {
        return if (it != null) {
            val values = it["value"]!! as Map<String, Int?>
            IntCriterionInput(
                values["value"] ?: 0,
                Optional.presentIfNotNull(values["value2"]),
                CriterionModifier.valueOf(it["modifier"]!! as String),
            )
        } else {
            null
        }
    }

    private fun convertFloatCriterionInput(it: Map<String, *>?): FloatCriterionInput? {
        return if (it != null) {
            val values = it["value"]!! as Map<String, Number?> // Might be an int or double
            FloatCriterionInput(
                values["value"]?.toDouble() ?: 0.0,
                Optional.presentIfNotNull(values["value2"]?.toDouble()),
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
        return (list as List<*>?)?.map { (it as Map<String, String>)["id"].orEmpty() }?.toList()
    }

    private fun convertHierarchicalMultiCriterionInput(it: Map<String, *>?): HierarchicalMultiCriterionInput? {
        return if (it != null) {
            val values = it["value"]!! as Map<String, *>
            val items = mapToIds(values["items"])
            val excludes = mapToIds(values["excluded"])
            HierarchicalMultiCriterionInput(
                Optional.presentIfNotNull(items),
                CriterionModifier.valueOf(it["modifier"]!!.toString()),
                Optional.presentIfNotNull(it["depth"] as Int?),
                Optional.presentIfNotNull(excludes),
            )
        } else {
            null
        }
    }

    private fun convertMultiCriterionInput(it: Map<String, *>?): MultiCriterionInput? {
        return if (it != null) {
            val items = mapToIds(it["items"])
            val excludes = mapToIds(it["excluded"])
            MultiCriterionInput(
                Optional.presentIfNotNull(items),
                CriterionModifier.valueOf(it["modifier"]!!.toString()),
                Optional.presentIfNotNull(excludes),
            )
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
            val value = it["value"].toString().uppercase().replace(" ", "_")
            GenderCriterionInput(
                Optional.presentIfNotNull(GenderEnum.valueOf(value)),
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
            val values = it["value"]!! as Map<String, String?>
            StashIDCriterionInput(
                Optional.presentIfNotNull(values["endpoint"]),
                Optional.presentIfNotNull(values["stash_id"]),
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
                Optional.presentIfNotNull(values["distance"].toString().toInt()),
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

    fun convertPerformerObjectFilter(f: Any?): PerformerFilterType? {
        return if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            PerformerFilterType(
                AND = Optional.presentIfNotNull(convertPerformerObjectFilter(filter.get("AND") as Map<String, Map<String, *>>?)),
                OR = Optional.presentIfNotNull(convertPerformerObjectFilter(filter.get("OR") as Map<String, Map<String, *>>?)),
                NOT = Optional.presentIfNotNull(convertPerformerObjectFilter(filter.get("NOT") as Map<String, Map<String, *>>?)),
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("name"))),
                disambiguation = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("disambiguation"))),
                details = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("details"))),
                filter_favorites = Optional.presentIfNotNull(convertBoolean(filter.get("filter_favorites"))),
                birth_year = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("birth_year"))),
                age = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("age"))),
                ethnicity = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("ethnicity"))),
                country = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("country"))),
                eye_color = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("eye_color"))),
                height_cm = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("height_cm"))),
                measurements = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("measurements"))),
                fake_tits = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("fake_tits"))),
                penis_length = Optional.presentIfNotNull(convertFloatCriterionInput(filter.get("penis_length"))),
                circumcised =
                    Optional.presentIfNotNull(
                        convertCircumcisionCriterionInput(
                            filter.get(
                                "circumcised",
                            ),
                        ),
                    ),
                career_length = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("career_length"))),
                tattoos = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("tattoos"))),
                piercings = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("piercings"))),
                aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("aliases"))),
                gender = Optional.presentIfNotNull(convertGenderCriterionInput(filter.get("gender"))),
                is_missing = Optional.presentIfNotNull(convertString(filter.get("is_missing"))),
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter.get("tags"))),
                tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("tag_count"))),
                scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("scene_count"))),
                image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("image_count"))),
                gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("gallery_count"))),
                o_counter = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("o_counter"))),
                stash_id_endpoint =
                    Optional.presentIfNotNull(
                        convertStashIDCriterionInput(
                            filter.get(
                                "stash_id_endpoint",
                            ),
                        ),
                    ),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("rating100"))),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("url"))),
                hair_color = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("hair_color"))),
                weight = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("weight"))),
                death_year = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("death_year"))),
                studios =
                    Optional.presentIfNotNull(
                        convertHierarchicalMultiCriterionInput(
                            filter.get(
                                "studios",
                            ),
                        ),
                    ),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter.get("performers"))),
                ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter.get("ignore_auto_tag"))),
                birthdate = Optional.presentIfNotNull(convertDateCriterionInput(filter.get("birthdate"))),
                death_date = Optional.presentIfNotNull(convertDateCriterionInput(filter.get("death_date"))),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("created_at"))),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("updated_at"))),
            )
        } else {
            null
        }
    }

    fun convertSceneObjectFilter(f: Any?): SceneFilterType? {
        return if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            SceneFilterType(
                AND = Optional.presentIfNotNull(convertSceneObjectFilter(filter.get("AND") as Map<String, Map<String, *>>?)),
                OR = Optional.presentIfNotNull(convertSceneObjectFilter(filter.get("OR") as Map<String, Map<String, *>>?)),
                NOT = Optional.presentIfNotNull(convertSceneObjectFilter(filter.get("NOT") as Map<String, Map<String, *>>?)),
                id = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("id"))),
                title = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("title"))),
                code = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("code"))),
                details = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("details"))),
                director = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("director"))),
                oshash = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("oshash"))),
                checksum = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("checksum"))),
                phash = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("phash"))),
                phash_distance =
                    Optional.presentIfNotNull(
                        convertPhashDistanceCriterionInput(
                            filter.get(
                                "phash_distance",
                            ),
                        ),
                    ),
                path = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("path"))),
                file_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("file_count"))),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("rating100"))),
                organized = Optional.presentIfNotNull(convertBoolean(filter.get("organized"))),
                o_counter = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("o_counter"))),
                duplicated =
                    Optional.presentIfNotNull(
                        convertPHashDuplicationCriterionInput(
                            filter.get(
                                "duplicated",
                            ),
                        ),
                    ),
                resolution = Optional.presentIfNotNull(convertResolutionCriterionInput(filter.get("resolution"))),
                framerate = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("framerate"))),
                video_codec = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("video_codec"))),
                audio_codec = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("audio_codec"))),
                duration = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("duration"))),
                has_markers = Optional.presentIfNotNull(convertString(filter.get("has_markers"))),
                is_missing = Optional.presentIfNotNull(convertString(filter.get("is_missing"))),
                studios =
                    Optional.presentIfNotNull(
                        convertHierarchicalMultiCriterionInput(
                            filter.get(
                                "studios",
                            ),
                        ),
                    ),
                movies = Optional.presentIfNotNull(convertMultiCriterionInput(filter.get("movies"))),
                tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter.get("tags"))),
                tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("tag_count"))),
                performer_tags =
                    Optional.presentIfNotNull(
                        convertHierarchicalMultiCriterionInput(
                            filter.get(
                                "performer_tags",
                            ),
                        ),
                    ),
                performer_favorite = Optional.presentIfNotNull(convertBoolean(filter.get("performer_favorite"))),
                performer_age = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("performer_age"))),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter.get("performers"))),
                performer_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("performer_count"))),
                stash_id_endpoint =
                    Optional.presentIfNotNull(
                        convertStashIDCriterionInput(
                            filter.get(
                                "stash_id_endpoint",
                            ),
                        ),
                    ),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("url"))),
                interactive = Optional.presentIfNotNull(convertBoolean(filter.get("interactive"))),
                interactive_speed = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("interactive_speed"))),
                captions = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("captions"))),
                resume_time = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("resume_time"))),
                play_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("play_count"))),
                play_duration = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("play_duration"))),
                date = Optional.presentIfNotNull(convertDateCriterionInput(filter.get("date"))),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("created_at"))),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("updated_at"))),
            )
        } else {
            null
        }
    }

    fun convertStudioObjectFilter(f: Any?): StudioFilterType? {
        return if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            StudioFilterType(
                AND = Optional.presentIfNotNull(convertStudioObjectFilter(filter.get("AND") as Map<String, Map<String, *>>?)),
                OR = Optional.presentIfNotNull(convertStudioObjectFilter(filter.get("OR") as Map<String, Map<String, *>>?)),
                NOT = Optional.presentIfNotNull(convertStudioObjectFilter(filter.get("NOT") as Map<String, Map<String, *>>?)),
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("name"))),
                details = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("details"))),
                parents = Optional.presentIfNotNull(convertMultiCriterionInput(filter.get("parents"))),
                stash_id_endpoint =
                    Optional.presentIfNotNull(
                        convertStashIDCriterionInput(
                            filter.get(
                                "stash_id_endpoint",
                            ),
                        ),
                    ),
                is_missing = Optional.presentIfNotNull(convertString(filter.get("is_missing"))),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("rating100"))),
                scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("scene_count"))),
                image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("image_count"))),
                gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("gallery_count"))),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("url"))),
                aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("aliases"))),
                ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter.get("ignore_auto_tag"))),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("created_at"))),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("updated_at"))),
            )
        } else {
            null
        }
    }

    fun convertTagObjectFilter(f: Any?): TagFilterType? {
        return if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            TagFilterType(
                AND = Optional.presentIfNotNull(convertTagObjectFilter(filter.get("AND") as Map<String, Map<String, *>>?)),
                OR = Optional.presentIfNotNull(convertTagObjectFilter(filter.get("OR") as Map<String, Map<String, *>>?)),
                NOT = Optional.presentIfNotNull(convertTagObjectFilter(filter.get("NOT") as Map<String, Map<String, *>>?)),
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("name"))),
                aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("aliases"))),
                description = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("description"))),
                is_missing = Optional.presentIfNotNull(convertString(filter.get("is_missing"))),
                scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("scene_count"))),
                image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("image_count"))),
                gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("gallery_count"))),
                performer_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("performer_count"))),
                marker_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("marker_count"))),
                parents =
                    Optional.presentIfNotNull(
                        convertHierarchicalMultiCriterionInput(
                            filter.get(
                                "parents",
                            ),
                        ),
                    ),
                children =
                    Optional.presentIfNotNull(
                        convertHierarchicalMultiCriterionInput(
                            filter.get(
                                "children",
                            ),
                        ),
                    ),
                parent_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("parent_count"))),
                child_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("child_count"))),
                ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter.get("ignore_auto_tag"))),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("created_at"))),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("updated_at"))),
            )
        } else {
            null
        }
    }

    fun convertMovieObjectFilter(f: Any?): MovieFilterType? {
        return if (f != null) {
            val filter = f as Map<String, Map<String, *>>
            MovieFilterType(
                name = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("name"))),
                director = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("director"))),
                synopsis = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("synopsis"))),
                duration = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("duration"))),
                rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("rating100"))),
                studios =
                    Optional.presentIfNotNull(
                        convertHierarchicalMultiCriterionInput(
                            filter?.get(
                                "studios",
                            ),
                        ),
                    ),
                is_missing = Optional.presentIfNotNull(convertString(filter?.get("is_missing"))),
                url = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("url"))),
                performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter?.get("performers"))),
                date = Optional.presentIfNotNull(convertDateCriterionInput(filter?.get("date"))),
                created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter?.get("created_at"))),
                updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter?.get("updated_at"))),
            )
        } else {
            null
        }
    }
}
