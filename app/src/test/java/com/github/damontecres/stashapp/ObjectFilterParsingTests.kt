package com.github.damontecres.stashapp

import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.Version
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

    /**
     * Get the SavedFilterData from a json file resource
     */
    private fun getSavedFilterData(file: String): SavedFilterData {
        val path = file.toPath()
        FileSystem.RESOURCES.read(path) {
            val jsonReader = BufferedSourceJsonReader(this)
            val response = FindSavedFilterQuery("1").parseJsonResponse(jsonReader)
            return response.data!!.findSavedFilter!!.savedFilterData
        }
    }

    @Test
    fun testSceneFilter() {
        val savedFilterData = getSavedFilterData("scene_savedfilter.json")
        val sceneFilter =
            FilterParser(Version.V0_25_0).convertSceneObjectFilter(savedFilterData.object_filter)
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
    fun testPerformerFilter() {
        val savedFilterData = getSavedFilterData("performer_savedfilter.json")
        val performerFilter =
            FilterParser(Version.V0_25_0).convertPerformerObjectFilter(savedFilterData.object_filter)
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
            listOf(GenderEnum.FEMALE),
            performerFilter.gender.getOrThrow()!!.value_list.getOrNull(),
        )
    }

    @Test
    fun testGenderFilter() {
        val savedFilterData = getSavedFilterData("gender_savedfilter.json")
        val performerFilter =
            FilterParser(Version.V0_25_0).convertPerformerObjectFilter(savedFilterData.object_filter)
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
            FilterParser(Version.V0_25_0).convertStudioObjectFilter(savedFilterData.object_filter)
        Assert.assertNotNull(studioFilter!!)
        Assert.assertEquals(FilterMode.STUDIOS, savedFilterData.mode)

        Assert.assertEquals(3, studioFilter.child_count.getOrThrow()!!.value)
    }
}
