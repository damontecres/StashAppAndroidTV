package com.github.damontecres.stashapp

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.filter.output.FilterWriter
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.OptionalSerializersModule
import com.github.damontecres.stashapp.util.Version
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

@RunWith(MockitoJUnitRunner::class)
class ObjectFilterParsingTests {
    private val filterParser = FilterParser(Version.MINIMUM_STASH_VERSION)

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
                            value = 100,
                            value2 = Optional.absent(),
                            modifier = CriterionModifier.INCLUDES_ALL,
                        ),
                    ),
            )
        val format = Json { serializersModule = OptionalSerializersModule }
        val json = format.encodeToString(SceneFilterType.serializer(), filter)
        val result = format.decodeFromString<SceneFilterType>(json)
        Assert.assertEquals(filter, result)
    }

    @Test
    fun testSceneFilter() {
        val savedFilterData = getSavedFilterData("scene_savedfilter.json")
        val sceneFilter =
            FilterParser(Version.MINIMUM_STASH_VERSION).convertSceneFilterType(savedFilterData.object_filter)
        Assert.assertNotNull(sceneFilter!!)
        Assert.assertEquals(FilterMode.SCENES, savedFilterData.mode)
        Assert.assertEquals(
            "33",
            sceneFilter.studios.getOrThrow()!!.value.getOrThrow()!!.first(),
        )
        Assert.assertEquals(
            listOf("9", "148"),
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
            listOf<String>("1"),
            sceneFilter.groups.getOrThrow()!!.value.getOrThrow()!!,
        )
        Assert.assertEquals(
            CriterionModifier.INCLUDES,
            sceneFilter.groups.getOrThrow()!!.modifier,
        )
    }

    @Test
    fun testScene2Filter() {
        val savedFilterData = getSavedFilterData("scene_savedfilter2.json")
        val sceneFilter =
            FilterParser(Version.MINIMUM_STASH_VERSION).convertSceneFilterType(savedFilterData.object_filter)
        Assert.assertNotNull(sceneFilter!!)
        Assert.assertEquals(FilterMode.SCENES, savedFilterData.mode)
    }

    @Test
    fun testPerformerFilter() {
        val savedFilterData = getSavedFilterData("performer_savedfilter.json")
        val performerFilter =
            FilterParser(Version.MINIMUM_STASH_VERSION).convertPerformerFilterType(savedFilterData.object_filter)
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
            FilterParser(Version.MINIMUM_STASH_VERSION).convertPerformerFilterType(savedFilterData.object_filter)
        Assert.assertNotNull(performerFilter!!)
        Assert.assertEquals(FilterMode.PERFORMERS, savedFilterData.mode)

        Assert.assertNull(performerFilter.gender.getOrThrow()!!.value.getOrNull())
        Assert.assertEquals(3, performerFilter.gender.getOrThrow()!!.value_list.getOrThrow()!!.size)
        Assert.assertTrue(
            performerFilter.gender.getOrThrow()!!.value_list.getOrThrow()!!
                .contains(GenderEnum.MALE),
        )
        Assert.assertTrue(
            performerFilter.gender.getOrThrow()!!.value_list.getOrThrow()!!
                .contains(GenderEnum.FEMALE),
        )
        Assert.assertTrue(
            performerFilter.gender.getOrThrow()!!.value_list.getOrThrow()!!
                .contains(GenderEnum.NON_BINARY),
        )
    }

    @Test
    fun testStudioChildrenFilter() {
        val savedFilterData = getSavedFilterData("studio_children_savedfilter.json")
        val studioFilter =
            FilterParser(Version.MINIMUM_STASH_VERSION).convertStudioFilterType(savedFilterData.object_filter)
        Assert.assertNotNull(studioFilter!!)
        Assert.assertEquals(FilterMode.STUDIOS, savedFilterData.mode)

        Assert.assertEquals(3, studioFilter.child_count.getOrThrow()!!.value)
    }

    /**
     * Test [FilterWriter] using a saved filter
     *
     * This basically does the following:
     * 1. Parses json into a filter using [FilterParser]
     * 2. Writes that filter out as a Map using [FilterWriter]
     * 3. Parse that Map using [FilterParser]
     * 4. Compares the filters from steps 1 & 3
     */
    private fun <T : StashDataFilter> checkFilterOutput(
        file: String,
        filterType: KClass<in T>,
        parser: (Any?) -> T?,
    ) {
        val savedFilterData = getSavedFilterData(file)
        val sceneFilter = parser(savedFilterData.object_filter)!!
        val filterWriter =
            FilterWriter { dataType, ids ->
                ids.associateWith { it }
            }
        val filterOut = runBlocking { filterWriter.convertFilter(sceneFilter) }
        val result = parser(filterOut)!!
        filterType.declaredMemberProperties.forEach { prop ->
            val expected = (prop.get(sceneFilter) as Optional<*>).getOrNull()
            val actual = (prop.get(result) as Optional<*>).getOrNull()
            when (expected) {
                is MultiCriterionInput -> {
                    expected as MultiCriterionInput
                    actual as MultiCriterionInput
                    Assert.assertTrue(prop.name, compareOptionalList(expected.value, actual.value))
                    Assert.assertTrue(
                        prop.name,
                        compareOptionalList(expected.excludes, actual.excludes),
                    )
                    Assert.assertEquals(prop.name, expected.modifier, actual.modifier)
                }

                is HierarchicalMultiCriterionInput -> {
                    expected as HierarchicalMultiCriterionInput
                    actual as HierarchicalMultiCriterionInput
                    Assert.assertTrue(prop.name, compareOptionalList(expected.value, actual.value))
                    Assert.assertTrue(
                        prop.name,
                        compareOptionalList(expected.excludes, actual.excludes),
                    )
                    Assert.assertEquals(prop.name, expected.modifier, actual.modifier)
                    Assert.assertEquals(prop.name, expected.depth, actual.depth)
                }

                else -> Assert.assertEquals(prop.name, expected, actual)
            }
        }
    }

    /**
     * When comparing HierarchicalMulti & Multi, there is little functional difference between an absent list and an empty list,
     * so override and check for that situation and return true if so
     */
    private fun compareOptionalList(
        expected: Optional<List<*>?>,
        actual: Optional<List<*>?>,
    ): Boolean {
        return if (expected == Optional.Absent && actual == Optional.Absent) {
            true
        } else {
            val exList = expected.getOrNull()
            val acList = actual.getOrNull()
            if (exList == acList) {
                true
            } else if (expected == Optional.Absent && acList != null && acList.isEmpty()) {
                true
            } else if (actual == Optional.Absent && exList != null && exList.isEmpty()) {
                true
            } else {
                false
            }
        }
    }

    @Test
    fun testSceneFilterWriter() {
        checkFilterOutput(
            "scene_savedfilter.json",
            SceneFilterType::class,
            filterParser::convertSceneFilterType,
        )
    }

    @Test
    fun testScene2FilterWriter() {
        checkFilterOutput(
            "scene_savedfilter2.json",
            SceneFilterType::class,
            filterParser::convertSceneFilterType,
        )
    }

    @Test
    fun testPerformerFilterWriter() {
        checkFilterOutput(
            "performer_savedfilter.json",
            PerformerFilterType::class,
            filterParser::convertPerformerFilterType,
        )
    }

    @Test
    fun testGenderFilterWriter() {
        checkFilterOutput(
            "gender_savedfilter.json",
            PerformerFilterType::class,
            filterParser::convertPerformerFilterType,
        )
    }

    @Test
    fun testStudioChildrenFilterWriter() {
        checkFilterOutput(
            "studio_children_savedfilter.json",
            StudioFilterType::class,
            filterParser::convertStudioFilterType,
        )
    }
}
