package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.Version

data class SortOption(
    val key: String,
    val nameStringId: Int,
    val requiresVersion: Version = Version.MINIMUM_STASH_VERSION,
)

val SCENE_SORT_OPTIONS =
    listOf(
        SortOption("title", R.string.stashapp_title),
        SortOption("path", R.string.stashapp_path),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("file_mod_time", R.string.stashapp_file_mod_time),
        SortOption("tag_count", R.string.stashapp_tag_count),
        SortOption("performer_count", R.string.stashapp_performer_count),
        SortOption("random", R.string.stashapp_random),
        SortOption("organized", R.string.stashapp_organized),
        SortOption("o_counter", R.string.stashapp_o_count),
        SortOption("date", R.string.stashapp_date),
        SortOption("file_count", R.string.stashapp_file_count),
        SortOption("filesize", R.string.stashapp_filesize),
        SortOption("duration", R.string.stashapp_duration),
        SortOption("framerate", R.string.stashapp_framerate),
        SortOption("bitrate", R.string.stashapp_bitrate),
        SortOption("last_played_at", R.string.stashapp_last_played_at),
        SortOption("resume_time", R.string.stashapp_resume_time),
        SortOption("play_duration", R.string.stashapp_play_duration),
        SortOption("play_count", R.string.stashapp_play_count),
        SortOption("movie_scene_number", R.string.stashapp_movie_scene_number),
        SortOption("interactive", R.string.stashapp_interactive),
        SortOption("interactive_speed", R.string.stashapp_interactive_speed),
        SortOption("perceptual_similarity", R.string.stashapp_perceptual_similarity),
    )

val GALLERY_SORT_OPTIONS =
    listOf(
        SortOption("title", R.string.stashapp_title),
        SortOption("path", R.string.stashapp_path),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("file_mod_time", R.string.stashapp_file_mod_time),
        SortOption("tag_count", R.string.stashapp_tag_count),
        SortOption("performer_count", R.string.stashapp_performer_count),
        SortOption("random", R.string.stashapp_random),
        SortOption("date", R.string.stashapp_date),
        SortOption("images_count", R.string.stashapp_image_count),
        SortOption("file_count", R.string.stashapp_zip_file_count),
    )

val IMAGE_SORT_OPTIONS =
    listOf(
        SortOption("title", R.string.stashapp_title),
        SortOption("path", R.string.stashapp_path),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("file_mod_time", R.string.stashapp_file_mod_time),
        SortOption("tag_count", R.string.stashapp_tag_count),
        SortOption("performer_count", R.string.stashapp_performer_count),
        SortOption("random", R.string.stashapp_random),
        SortOption("o_counter", R.string.stashapp_o_count),
        SortOption("filesize", R.string.stashapp_filesize),
        SortOption("file_count", R.string.stashapp_file_count),
        SortOption("date", R.string.stashapp_date),
    )

val MOVIE_SORT_OPTIONS =
    listOf(
        SortOption("name", R.string.stashapp_name),
        SortOption("random", R.string.stashapp_random),
        SortOption("date", R.string.stashapp_date),
        SortOption("duration", R.string.stashapp_duration),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("scenes_count", R.string.stashapp_scene_count),
    )

val PERFORMER_SORT_OPTIONS =
    listOf(
        SortOption("name", R.string.stashapp_name),
        SortOption("height", R.string.stashapp_height),
        SortOption("birthdate", R.string.stashapp_birthdate),
        SortOption("tag_count", R.string.stashapp_tag_count),
        SortOption("random", R.string.stashapp_random),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("penis_length", R.string.stashapp_penis_length),
        SortOption("scenes_count", R.string.stashapp_scene_count),
        SortOption("images_count", R.string.stashapp_image_count),
        SortOption("galleries_count", R.string.stashapp_gallery_count),
        SortOption("o_counter", R.string.stashapp_o_count),
        SortOption("play_count", R.string.stashapp_play_count, Version.V0_26_0),
        SortOption("last_played_at", R.string.stashapp_last_played_at, Version.V0_26_0),
        SortOption("last_o_at", R.string.stashapp_last_o_at, Version.V0_26_0),
    )

val MARKER_SORT_OPTIONS =
    listOf(
        SortOption("title", R.string.stashapp_title),
        SortOption("seconds", R.string.stashapp_seconds),
        SortOption("scene_id", R.string.stashapp_scene_id),
        SortOption("random", R.string.stashapp_random),
        SortOption("scenes_updated_at", R.string.stashapp_scenes_updated_at),
    )

val STUDIO_SORT_OPTIONS =
    listOf(
        SortOption("name", R.string.stashapp_name),
        SortOption("random", R.string.stashapp_random),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("scenes_count", R.string.stashapp_scene_count),
        SortOption("images_count", R.string.stashapp_image_count),
        SortOption("galleries_count", R.string.stashapp_gallery_count),
    )

val TAG_SORT_OPTIONS =
    listOf(
        SortOption("name", R.string.stashapp_name),
        SortOption("random", R.string.stashapp_random),
        SortOption("scenes_count", R.string.stashapp_scene_count),
        SortOption("images_count", R.string.stashapp_image_count),
        SortOption("galleries_count", R.string.stashapp_gallery_count),
        SortOption("performers_count", R.string.stashapp_performer_count),
        SortOption("scene_markers_count", R.string.stashapp_marker_count),
    )
