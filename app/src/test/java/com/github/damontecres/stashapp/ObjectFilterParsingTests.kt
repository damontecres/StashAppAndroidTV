package com.github.damontecres.stashapp

import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FilterMode
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert
import org.junit.Test

class ObjectFilterParsingTests {

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
            convertSceneObjectFilter(savedFilterData.object_filter)
        Assert.assertNotNull(sceneFilter!!)
        Assert.assertEquals(FilterMode.SCENES, savedFilterData.mode)
        Assert.assertEquals(
            "33",
            sceneFilter.studios.getOrThrow()!!.value.getOrThrow()!!.first()
        )
        Assert.assertEquals(
            listOf("8", "148"),
            sceneFilter.tags.getOrThrow()!!.value.getOrThrow()!!
        )
        Assert.assertEquals(
            1,
            sceneFilter.play_count.getOrThrow()!!.value
        )
        Assert.assertEquals(
            4,
            sceneFilter.resume_time.getOrThrow()!!.value
        )
        Assert.assertEquals(
            9,
            sceneFilter.resume_time.getOrThrow()!!.value2.getOrThrow()!!
        )
        Assert.assertEquals(
            "2024-01-01 23:00",
            sceneFilter.updated_at.getOrThrow()!!.value
        )
    }

    @Test
    fun testPerformerFilter() {
        val savedFilterData = getSavedFilterData("performer_savedfilter.json")
        val performerFilter = convertPerformerObjectFilter(savedFilterData.object_filter)
        Assert.assertNotNull(performerFilter!!)
        Assert.assertEquals(FilterMode.PERFORMERS, savedFilterData.mode)

        Assert.assertEquals(3, performerFilter.tags.getOrThrow()!!.value.getOrThrow()!!.size)
        Assert.assertEquals(3, performerFilter.studios.getOrThrow()!!.value.getOrThrow()!!.size)
        Assert.assertEquals(1, performerFilter.studios.getOrThrow()!!.excludes.getOrThrow()!!.size)
        Assert.assertEquals(
            "94",
            performerFilter.studios.getOrThrow()!!.excludes.getOrThrow()!!.first()
        )
    }
}
