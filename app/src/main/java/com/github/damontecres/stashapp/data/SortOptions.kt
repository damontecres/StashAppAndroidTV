package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.Version

data class SortOption(
    val key: String,
    val nameStringId: Int,
    val requiresVersion: Version = Version.MINIMUM_STASH_VERSION,
)

val COMMON_SORT_OPTIONS =
    arrayOf(
        SortOption("created_at", R.string.stashapp_created_at),
        SortOption("updated_at", R.string.stashapp_updated_at),
        SortOption("random", R.string.stashapp_random),
    )

val SCENE_SORT_OPTIONS =
    listOf(
        *COMMON_SORT_OPTIONS,
        SortOption("bitrate", R.string.stashapp_bitrate),
        SortOption("date", R.string.stashapp_date),
        SortOption("duration", R.string.stashapp_duration),
        SortOption("file_count", R.string.stashapp_file_count),
        SortOption("file_mod_time", R.string.stashapp_file_mod_time),
        SortOption("filesize", R.string.stashapp_filesize),
        SortOption("framerate", R.string.stashapp_framerate),
        SortOption("interactive", R.string.stashapp_interactive),
        SortOption("interactive_speed", R.string.stashapp_interactive_speed),
        SortOption("last_o_at", R.string.stashapp_last_o_at, Version.V0_26_0),
        SortOption("last_played_at", R.string.stashapp_last_played_at),
        SortOption("movie_scene_number", R.string.stashapp_group_scene_number),
        SortOption("o_counter", R.string.stashapp_o_count),
        SortOption("organized", R.string.stashapp_organized),
        SortOption("path", R.string.stashapp_path),
        SortOption("perceptual_similarity", R.string.stashapp_perceptual_similarity),
        SortOption("performer_count", R.string.stashapp_performer_count),
        SortOption("play_count", R.string.stashapp_play_count),
        SortOption("play_duration", R.string.stashapp_play_duration),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("resume_time", R.string.stashapp_resume_time),
        SortOption("tag_count", R.string.stashapp_tag_count),
        SortOption("title", R.string.stashapp_title),
    )

val GALLERY_SORT_OPTIONS =
    listOf(
        *COMMON_SORT_OPTIONS,
        SortOption("date", R.string.stashapp_date),
        SortOption("file_count", R.string.stashapp_zip_file_count),
        SortOption("file_mod_time", R.string.stashapp_file_mod_time),
        SortOption("images_count", R.string.stashapp_image_count),
        SortOption("path", R.string.stashapp_path),
        SortOption("performer_count", R.string.stashapp_performer_count),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("tag_count", R.string.stashapp_tag_count),
        SortOption("title", R.string.stashapp_title),
    )

val IMAGE_SORT_OPTIONS =
    listOf(
        *COMMON_SORT_OPTIONS,
        SortOption("date", R.string.stashapp_date),
        SortOption("file_count", R.string.stashapp_file_count),
        SortOption("file_mod_time", R.string.stashapp_file_mod_time),
        SortOption("filesize", R.string.stashapp_filesize),
        SortOption("o_counter", R.string.stashapp_o_count),
        SortOption("path", R.string.stashapp_path),
        SortOption("performer_count", R.string.stashapp_performer_count),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("tag_count", R.string.stashapp_tag_count),
        SortOption("title", R.string.stashapp_title),
    )

val MOVIE_SORT_OPTIONS =
    listOf(
        *COMMON_SORT_OPTIONS,
        SortOption("date", R.string.stashapp_date),
        SortOption("duration", R.string.stashapp_duration),
        SortOption("name", R.string.stashapp_name),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("scenes_count", R.string.stashapp_scene_count),
    )

val PERFORMER_SORT_OPTIONS =
    listOf(
        *COMMON_SORT_OPTIONS,
        SortOption("birthdate", R.string.stashapp_birthdate),
        SortOption("galleries_count", R.string.stashapp_gallery_count),
        SortOption("height", R.string.stashapp_height),
        SortOption("images_count", R.string.stashapp_image_count),
        SortOption("last_o_at", R.string.stashapp_last_o_at, Version.V0_26_0),
        SortOption("last_played_at", R.string.stashapp_last_played_at, Version.V0_26_0),
        SortOption("name", R.string.stashapp_name),
        SortOption("o_counter", R.string.stashapp_o_count),
        SortOption("penis_length", R.string.stashapp_penis_length),
        SortOption("play_count", R.string.stashapp_play_count, Version.V0_26_0),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("scenes_count", R.string.stashapp_scene_count),
        SortOption("tag_count", R.string.stashapp_tag_count),
        SortOption("career_length", R.string.stashapp_career_length, Version.V0_27_0),
        SortOption("measurements", R.string.stashapp_measurements, Version.V0_27_0),
        SortOption("weight", R.string.stashapp_weight, Version.V0_27_0),
    )

val MARKER_SORT_OPTIONS =
    listOf(
        *COMMON_SORT_OPTIONS,
        SortOption("scene_id", R.string.stashapp_scene_id),
        SortOption("scenes_updated_at", R.string.stashapp_scenes_updated_at),
        SortOption("seconds", R.string.stashapp_seconds),
        SortOption("title", R.string.stashapp_title),
    )

val STUDIO_SORT_OPTIONS =
    listOf(
        *COMMON_SORT_OPTIONS,
        SortOption("child_count", R.string.stashapp_subsidiary_studio_count),
        SortOption("galleries_count", R.string.stashapp_gallery_count),
        SortOption("images_count", R.string.stashapp_image_count),
        SortOption("name", R.string.stashapp_name),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("scenes_count", R.string.stashapp_scene_count),
    )

val TAG_SORT_OPTIONS =
    listOf(
        *COMMON_SORT_OPTIONS,
        SortOption("galleries_count", R.string.stashapp_gallery_count),
        SortOption("images_count", R.string.stashapp_image_count),
        SortOption("name", R.string.stashapp_name),
        SortOption("performers_count", R.string.stashapp_performer_count),
        SortOption("scene_markers_count", R.string.stashapp_marker_count),
        SortOption("scenes_count", R.string.stashapp_scene_count),
    )
