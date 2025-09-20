package com.github.damontecres.stashapp.util

import com.github.damontecres.stashapp.data.DataType

/**
 * List of keys to look up how to sort items in a tab on a page
 *
 * https://github.com/stashapp/stash/blob/develop/ui/v2.5/src/components/List/views.ts
 */
enum class PageFilterKey(
    val dataType: DataType,
    val prefKey: String,
) {
    TAG_MARKERS(DataType.MARKER, "tag_markers"),
    TAG_GALLERIES(DataType.GALLERY, "tag_galleries"),
    TAG_SCENES(DataType.SCENE, "tag_scenes"),
    TAG_IMAGES(DataType.IMAGE, "tag_images"),
    TAG_PERFORMERS(DataType.PERFORMER, "tag_performers"),

    PERFORMER_SCENES(DataType.SCENE, "performer_scenes"),
    PERFORMER_GALLERIES(DataType.GALLERY, "performer_galleries"),
    PERFORMER_IMAGES(DataType.IMAGE, "performer_images"),
    PERFORMER_GROUPS(DataType.GROUP, "performer_groups"),
    PERFORMER_APPEARS_WITH(DataType.PERFORMER, "performer_appears_with"),

    STUDIO_GALLERIES(DataType.GALLERY, "studio_galleries"),
    STUDIO_IMAGES(DataType.IMAGE, "studio_images"),

    GALLERY_IMAGES(DataType.IMAGE, "gallery_images"),

    STUDIO_SCENES(DataType.SCENE, "studio_scenes"),
    STUDIO_GROUPS(DataType.GROUP, "studio_groups"),
    STUDIO_PERFORMERS(DataType.PERFORMER, "studio_performers"),
    STUDIO_CHILDREN(DataType.STUDIO, "studio_children"),

    GROUP_SCENES(DataType.SCENE, "group_scenes"),
    GROUP_SUB_GROUPS(DataType.GROUP, "group_sub_groups"),
    GROUP_PERFORMERS(DataType.PERFORMER, "group_performers"),
}
