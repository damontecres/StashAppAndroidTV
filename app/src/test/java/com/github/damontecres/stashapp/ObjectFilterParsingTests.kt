package com.github.damontecres.stashapp

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.optionalSerializerModule
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ObjectFilterParsingTests {
    @Before
    fun init() {
    }

    companion object {
        /**
         * Get the SavedFilterData from a json file resource
         */
        fun getSavedFilterData(file: String): SavedFilterData {
            val path = file.toPath()
            FileSystem.RESOURCES.read(path) {
                val jsonReader = BufferedSourceJsonReader(this)
                val response = FindSavedFilterQuery("1").parseJsonResponse(jsonReader)
                return response.data!!.findSavedFilter!!.savedFilterData
            }
        }
    }

    @Test
    fun testJson() {
        val filter =
            SceneFilterType(
                file_count =
                    Optional.present(
                        IntCriterionInput(
                            100,
                            Optional.absent(),
                            CriterionModifier.INCLUDES_ALL,
                        ),
                    ),
            )
        val format = Json { serializersModule = optionalSerializerModule }
        val json = format.encodeToString(SceneFilterType.serializer(), filter)
        val result = format.decodeFromString<SceneFilterType>(json)
        Assert.assertEquals(filter, result)
    }

    @Test
    fun testSceneFilter() {
        val savedFilterData = getSavedFilterData("scene_savedfilter.json")
        val sceneFilter =
            FilterParser(Version.MINIMUM_STASH_VERSION).convertSceneObjectFilter(savedFilterData.object_filter)
        Assert.assertNotNull(sceneFilter!!)
        Assert.assertEquals(FilterMode.SCENES, savedFilterData.mode)
        Assert.assertEquals(
            "33",
            sceneFilter.studios.getOrThrow()!!.value.getOrThrow()!!.first(),
        )
        Assert.assertEquals(
            listOf("8", "148"),
            sceneFilter.tags.getOrThrow()!!.value.getOrThrow()!!,
        )
        Assert.assertEquals(
            1,
            sceneFilter.play_count.getOrThrow()!!.value,
        )
        Assert.assertEquals(
            4,
            sceneFilter.resume_time.getOrThrow()!!.value,
        )
        Assert.assertEquals(
            9,
            sceneFilter.resume_time.getOrThrow()!!.value2.getOrThrow()!!,
        )
        Assert.assertEquals(
            "2024-01-01 23:00",
            sceneFilter.updated_at.getOrThrow()!!.value,
        )
        Assert.assertEquals(
            listOf("1131"),
            sceneFilter.performers.getOrThrow()!!.value.getOrThrow()!!,
        )
        Assert.assertEquals(
            listOf<String>(),
            sceneFilter.movies.getOrThrow()!!.value.getOrThrow()!!,
        )
        Assert.assertEquals(
            CriterionModifier.NOT_NULL,
            sceneFilter.movies.getOrThrow()!!.modifier,
        )
    }

    @Test
    fun testScene2Filter() {
        val savedFilterData = getSavedFilterData("scene_savedfilter2.json")
        val sceneFilter =
            FilterParser(Version.MINIMUM_STASH_VERSION).convertSceneObjectFilter(savedFilterData.object_filter)
        Assert.assertNotNull(sceneFilter!!)
        Assert.assertEquals(FilterMode.SCENES, savedFilterData.mode)
    }

    @Test
    fun testPerformerFilter() {
        val savedFilterData = getSavedFilterData("performer_savedfilter.json")
        val performerFilter =
            FilterParser(Version.MINIMUM_STASH_VERSION).convertPerformerObjectFilter(savedFilterData.object_filter)
        Assert.assertNotNull(performerFilter!!)
        Assert.assertEquals(FilterMode.PERFORMERS, savedFilterData.mode)

        Assert.assertEquals(3, performerFilter.tags.getOrThrow()!!.value.getOrThrow()!!.size)
        Assert.assertEquals(3, performerFilter.studios.getOrThrow()!!.value.getOrThrow()!!.size)
        Assert.assertEquals(1, performerFilter.studios.getOrThrow()!!.excludes.getOrThrow()!!.size)
        Assert.assertEquals(
            "94",
            performerFilter.studios.getOrThrow()!!.excludes.getOrThrow()!!.first(),
        )
        Assert.assertNull(performerFilter.gender.getOrThrow()!!.value.getOrNull())
        Assert.assertEquals(
            listOf(GenderEnum.MALE, GenderEnum.FEMALE),
            performerFilter.gender.getOrThrow()!!.value_list.getOrNull(),
        )
    }

    @Test
    fun testGenderFilter() {
        val savedFilterData = getSavedFilterData("gender_savedfilter.json")
        val performerFilter =
            FilterParser(Version.MINIMUM_STASH_VERSION).convertPerformerObjectFilter(savedFilterData.object_filter)
        Assert.assertNotNull(performerFilter!!)
        Assert.assertEquals(FilterMode.PERFORMERS, savedFilterData.mode)

        Assert.assertNull(performerFilter.gender.getOrThrow()!!.value.getOrNull())
        Assert.assertEquals(2, performerFilter.gender.getOrThrow()!!.value_list.getOrThrow()!!.size)
        Assert.assertTrue(
            performerFilter.gender.getOrThrow()!!.value_list.getOrThrow()!!
                .contains(GenderEnum.MALE),
        )
        Assert.assertTrue(
            performerFilter.gender.getOrThrow()!!.value_list.getOrThrow()!!
                .contains(GenderEnum.FEMALE),
        )
    }

    @Test
    fun testStudioChildrenFilter() {
        val savedFilterData = getSavedFilterData("studio_children_savedfilter.json")
        val studioFilter =
            FilterParser(Version.MINIMUM_STASH_VERSION).convertStudioObjectFilter(savedFilterData.object_filter)
        Assert.assertNotNull(studioFilter!!)
        Assert.assertEquals(FilterMode.STUDIOS, savedFilterData.mode)

        Assert.assertEquals(3, studioFilter.child_count.getOrThrow()!!.value)
    }
}
