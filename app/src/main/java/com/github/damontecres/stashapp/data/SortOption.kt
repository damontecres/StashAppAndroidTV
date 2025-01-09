package com.github.damontecres.stashapp.data

import android.content.Context
import androidx.annotation.StringRes
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.Version
import kotlinx.serialization.Serializable
import kotlin.reflect.full.isSubclassOf

/**
 * A way to sort something
 */
@Serializable
sealed interface SortOption {
    /**
     * The key as understood by the server
     */
    val key: String

    /**
     * The minimum server version required to sort by this
     *
     * This allows for adding future compatible sorting to older app versions
     */
    val requiresVersion: Version

    /**
     * Get the readable representation of the sort
     */
    fun getString(context: Context): String

    @Serializable
    sealed class SortOptionImpl(
        override val key: String,
        /**
         * The string resource for the readable name of the sort
         */
        @StringRes val nameStringId: Int?,
        override val requiresVersion: Version = Version.MINIMUM_STASH_VERSION,
    ) : SortOption {
        override fun getString(context: Context): String = if (nameStringId != null) context.getString(nameStringId) else key
    }

    @Serializable
    data object Birthdate : SortOptionImpl("birthdate", R.string.stashapp_birthdate)

    @Serializable
    data object Bitrate : SortOptionImpl("bitrate", R.string.stashapp_bitrate)

    @Serializable
    data object CareerLength :
        SortOptionImpl("career_length", R.string.stashapp_career_length, Version.V0_27_0)

    @Serializable
    data object ChildCount :
        SortOptionImpl("child_count", R.string.stashapp_subsidiary_studio_count)

    @Serializable
    data object CreatedAt : SortOptionImpl("created_at", R.string.stashapp_created_at)

    @Serializable
    data object Date : SortOptionImpl("date", R.string.stashapp_date)

    @Serializable
    data object Duration : SortOptionImpl("duration", R.string.stashapp_duration)

    @Serializable
    data object FileCount : SortOptionImpl("file_count", R.string.stashapp_file_count)

    @Serializable
    data object FileModTime : SortOptionImpl("file_mod_time", R.string.stashapp_file_mod_time)

    @Serializable
    data object FileSize : SortOptionImpl("filesize", R.string.stashapp_filesize)

    @Serializable
    data object FrameRate : SortOptionImpl("framerate", R.string.stashapp_framerate)

    @Serializable
    data object GalleriesCount : SortOptionImpl("galleries_count", R.string.stashapp_gallery_count)

    @Serializable
    data object GroupSceneNumber :
        SortOptionImpl("group_scene_number", R.string.stashapp_group_scene_number)

    @Serializable
    data object Height : SortOptionImpl("height", R.string.stashapp_height)

    @Serializable
    data object ImagesCount : SortOptionImpl("images_count", R.string.stashapp_image_count)

    @Serializable
    data object InteractiveSpeed :
        SortOptionImpl("interactive_speed", R.string.stashapp_interactive_speed)

    @Serializable
    data object Interactive : SortOptionImpl("interactive", R.string.stashapp_interactive)

    @Serializable
    data object LastOAt : SortOptionImpl("last_o_at", R.string.stashapp_last_o_at, Version.V0_26_0)

    @Serializable
    data object LastPlayedAt :
        SortOptionImpl("last_played_at", R.string.stashapp_last_played_at, Version.V0_26_0)

    @Serializable
    data object Measurements :
        SortOptionImpl("measurements", R.string.stashapp_measurements, Version.V0_27_0)

    @Serializable
    data object Name : SortOptionImpl("name", R.string.stashapp_name)

    @Serializable
    data object OCounter : SortOptionImpl("o_counter", R.string.stashapp_o_count)

    @Serializable
    data object Organized : SortOptionImpl("organized", R.string.stashapp_organized)

    @Serializable
    data object Path : SortOptionImpl("path", R.string.stashapp_path)

    @Serializable
    data object PenisLength : SortOptionImpl("penis_length", R.string.stashapp_penis_length)

    @Serializable
    data object PerceptualSimilarity :
        SortOptionImpl("perceptual_similarity", R.string.stashapp_perceptual_similarity)

    @Serializable
    data object PerformerCount :
        SortOptionImpl("performer_count", R.string.stashapp_performer_count)

    @Serializable
    data object PerformersCount :
        SortOptionImpl("performers_count", R.string.stashapp_performer_count)

    @Serializable
    data object PlayCount :
        SortOptionImpl("play_count", R.string.stashapp_play_count, Version.V0_26_0)

    @Serializable
    data object PlayDuration : SortOptionImpl("play_duration", R.string.stashapp_play_duration)

    @Serializable
    data object Random : SortOptionImpl("random", R.string.stashapp_random)

    @Serializable
    data object Rating : SortOptionImpl("rating", R.string.stashapp_rating)

    @Serializable
    data object ResumeTime : SortOptionImpl("resume_time", R.string.stashapp_resume_time)

    @Serializable
    data object SceneId : SortOptionImpl("scene_id", R.string.stashapp_scene_id)

    @Serializable
    data object SceneMarkersCount :
        SortOptionImpl("scene_markers_count", R.string.stashapp_marker_count)

    @Serializable
    data object ScenesCount : SortOptionImpl("scenes_count", R.string.stashapp_scene_count)

    @Serializable
    data object ScenesUpdatedAt :
        SortOptionImpl("scenes_updated_at", R.string.stashapp_scenes_updated_at)

    @Serializable
    data object Seconds : SortOptionImpl("seconds", R.string.stashapp_seconds)

    @Serializable
    data object TagCount : SortOptionImpl("tag_count", R.string.stashapp_tag_count)

    @Serializable
    data object Title : SortOptionImpl("title", R.string.stashapp_title)

    @Serializable
    data object UpdatedAt : SortOptionImpl("updated_at", R.string.stashapp_updated_at)

    @Serializable
    data object Weight : SortOptionImpl("weight", R.string.stashapp_weight, Version.V0_27_0)

    /**
     * Represents an unknown sort key possibly from a future server version
     */
    @Serializable
    data class Unknown(
        override val key: String,
        override val requiresVersion: Version = Version.MINIMUM_STASH_VERSION,
    ) : SortOption {
        override fun getString(context: Context): String = key
    }

    companion object {
        private val MAPPING =
            SortOption::class
                .nestedClasses
                .filter { klass -> klass.isSubclassOf(SortOption::class) }
                .mapNotNull { klass -> klass.objectInstance }
                .filterIsInstance<SortOption>()
                .associateBy { it.key }

        fun getByKey(key: String): SortOption =
            if (key.startsWith("random")) {
                Random
            } else {
                MAPPING[key] ?: Unknown(key)
            }

        /**
         * Whether jumping alphabetical is sorted
         */
        fun isJumpSupported(
            dataType: DataType,
            sortOption: SortOption,
        ): Boolean =
            dataType in
                setOf(
                    DataType.TAG,
                    DataType.GROUP,
                    DataType.PERFORMER,
                    DataType.STUDIO,
                ) &&
                sortOption == Name

        private val COMMON_SORT_OPTIONS =
            arrayOf(
                CreatedAt,
                UpdatedAt,
                Random,
            )

        val SCENE_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                Bitrate,
                Date,
                Duration,
                FileCount,
                FileModTime,
                FileSize,
                FrameRate,
                Interactive,
                InteractiveSpeed,
                LastOAt,
                LastPlayedAt,
                GroupSceneNumber,
                OCounter,
                Organized,
                Path,
                PerceptualSimilarity,
                PerformerCount,
                PlayCount,
                PlayDuration,
                Rating,
                ResumeTime,
                TagCount,
                Title,
            )

        val GALLERY_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                Date,
                FileCount,
                FileModTime,
                ImagesCount,
                Path,
                PerformerCount,
                Rating,
                TagCount,
                Title,
            )

        val IMAGE_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                Date,
                FileCount,
                FileModTime,
                FileSize,
                OCounter,
                Path,
                PerformerCount,
                Rating,
                TagCount,
                Title,
            )

        val GROUP_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                Date,
                Duration,
                Name,
                Rating,
                ScenesCount,
            )

        val PERFORMER_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                Birthdate,
                GalleriesCount,
                Height,
                ImagesCount,
                LastOAt,
                LastPlayedAt,
                Name,
                OCounter,
                PenisLength,
                PlayCount,
                Rating,
                ScenesCount,
                TagCount,
                CareerLength,
                Measurements,
                Weight,
            )

        val MARKER_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                SceneId,
                ScenesUpdatedAt,
                Seconds,
                Title,
            )

        val STUDIO_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                ChildCount,
                GalleriesCount,
                ImagesCount,
                Name,
                Rating,
                ScenesCount,
            )

        val TAG_SORT_OPTIONS =
            listOf(
                *COMMON_SORT_OPTIONS,
                GalleriesCount,
                ImagesCount,
                Name,
                PerformersCount,
                SceneMarkersCount,
                ScenesCount,
            )
    }
}
