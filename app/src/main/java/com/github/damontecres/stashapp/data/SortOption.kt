package com.github.damontecres.stashapp.data

import androidx.annotation.StringRes
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.Version

/**
 * A way to sort something
 */
enum class SortOption(
    /**
     * The key as understood by the server
     */
    val key: String,
    /**
     * The string resource for the readable name of the sort
     */
    @StringRes val nameStringId: Int,
    /**
     * The minimum server version required to sort by this
     *
     * This allows for adding future compatible sorting to older app versions
     */
    val requiresVersion: Version = Version.MINIMUM_STASH_VERSION,
) {
    BIRTHDATE("birthdate", R.string.stashapp_birthdate),
    BITRATE("bitrate", R.string.stashapp_bitrate),
    CAREER_LENGTH("career_length", R.string.stashapp_career_length, Version.V0_27_0),
    CHILD_COUNT("child_count", R.string.stashapp_subsidiary_studio_count),
    CREATED_AT("created_at", R.string.stashapp_created_at),
    DATE("date", R.string.stashapp_date),
    DURATION("duration", R.string.stashapp_duration),
    FILE_COUNT("file_count", R.string.stashapp_file_count),
    FILE_MOD_TIME("file_mod_time", R.string.stashapp_file_mod_time),
    FILESIZE("filesize", R.string.stashapp_filesize),
    FRAMERATE("framerate", R.string.stashapp_framerate),
    GALLERIES_COUNT("galleries_count", R.string.stashapp_gallery_count),
    GROUP_SCENE_NUMBER("group_scene_number", R.string.stashapp_group_scene_number),
    HEIGHT("height", R.string.stashapp_height),
    IMAGES_COUNT("images_count", R.string.stashapp_image_count),
    INTERACTIVE_SPEED("interactive_speed", R.string.stashapp_interactive_speed),
    INTERACTIVE("interactive", R.string.stashapp_interactive),
    LAST_O_AT("last_o_at", R.string.stashapp_last_o_at, Version.V0_26_0),
    LAST_PLAYED_AT("last_played_at", R.string.stashapp_last_played_at, Version.V0_26_0),
    MEASUREMENTS("measurements", R.string.stashapp_measurements, Version.V0_27_0),
    NAME("name", R.string.stashapp_name),
    O_COUNTER("o_counter", R.string.stashapp_o_count),
    ORGANIZED("organized", R.string.stashapp_organized),
    PATH("path", R.string.stashapp_path),
    PENIS_LENGTH("penis_length", R.string.stashapp_penis_length),
    PERCEPTUAL_SIMILARITY("perceptual_similarity", R.string.stashapp_perceptual_similarity),
    PERFORMER_COUNT("performer_count", R.string.stashapp_performer_count),
    PERFORMERS_COUNT("performers_count", R.string.stashapp_performer_count),
    PLAY_COUNT("play_count", R.string.stashapp_play_count, Version.V0_26_0),
    PLAY_DURATION("play_duration", R.string.stashapp_play_duration),
    RANDOM("random", R.string.stashapp_random),
    RATING("rating", R.string.stashapp_rating),
    RESUME_TIME("resume_time", R.string.stashapp_resume_time),
    SCENE_ID("scene_id", R.string.stashapp_scene_id),
    SCENE_MARKERS_COUNT("scene_markers_count", R.string.stashapp_marker_count),
    SCENES_COUNT("scenes_count", R.string.stashapp_scene_count),
    SCENES_UPDATED_AT("scenes_updated_at", R.string.stashapp_scenes_updated_at),
    SECONDS("seconds", R.string.stashapp_seconds),
    TAG_COUNT("tag_count", R.string.stashapp_tag_count),
    TITLE("title", R.string.stashapp_title),
    UPDATED_AT("updated_at", R.string.stashapp_updated_at),
    WEIGHT("weight", R.string.stashapp_weight, Version.V0_27_0),
    ;

    companion object {
        fun getByKey(key: String): SortOption {
            return if (key.startsWith("random")) {
                RANDOM
            } else {
                entries.first { it.key == key }
            }
        }

        private val COMMON_SORT_OPTIONS =
            arrayOf(
                CREATED_AT,
                UPDATED_AT,
                RANDOM,
            )

        val SCENE_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                BITRATE,
                DATE,
                DURATION,
                FILE_COUNT,
                FILE_MOD_TIME,
                FILESIZE,
                FRAMERATE,
                INTERACTIVE,
                INTERACTIVE_SPEED,
                LAST_O_AT,
                LAST_PLAYED_AT,
                GROUP_SCENE_NUMBER,
                O_COUNTER,
                ORGANIZED,
                PATH,
                PERCEPTUAL_SIMILARITY,
                PERFORMER_COUNT,
                PLAY_COUNT,
                PLAY_DURATION,
                RATING,
                RESUME_TIME,
                TAG_COUNT,
                TITLE,
            )

        val GALLERY_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                DATE,
                FILE_COUNT,
                FILE_MOD_TIME,
                IMAGES_COUNT,
                PATH,
                PERFORMER_COUNT,
                RATING,
                TAG_COUNT,
                TITLE,
            )

        val IMAGE_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                DATE,
                FILE_COUNT,
                FILE_MOD_TIME,
                FILESIZE,
                O_COUNTER,
                PATH,
                PERFORMER_COUNT,
                RATING,
                TAG_COUNT,
                TITLE,
            )

        val GROUP_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                DATE,
                DURATION,
                NAME,
                RATING,
                SCENES_COUNT,
            )

        val PERFORMER_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                BIRTHDATE,
                GALLERIES_COUNT,
                HEIGHT,
                IMAGES_COUNT,
                LAST_O_AT,
                LAST_PLAYED_AT,
                NAME,
                O_COUNTER,
                PENIS_LENGTH,
                PLAY_COUNT,
                RATING,
                SCENES_COUNT,
                TAG_COUNT,
                CAREER_LENGTH,
                MEASUREMENTS,
                WEIGHT,
            )

        val MARKER_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                SCENE_ID,
                SCENES_UPDATED_AT,
                SECONDS,
                TITLE,
            )

        val STUDIO_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                CHILD_COUNT,
                GALLERIES_COUNT,
                IMAGES_COUNT,
                NAME,
                RATING,
                SCENES_COUNT,
            )

        val TAG_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                GALLERIES_COUNT,
                IMAGES_COUNT,
                NAME,
                PERFORMERS_COUNT,
                SCENE_MARKERS_COUNT,
                SCENES_COUNT,
            )
    }
}
