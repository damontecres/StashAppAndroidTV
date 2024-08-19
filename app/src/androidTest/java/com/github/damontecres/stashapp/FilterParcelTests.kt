package com.github.damontecres.stashapp

import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationEnum
import com.github.damontecres.stashapp.api.type.PhashDistanceCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionEnum
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput
import com.github.damontecres.stashapp.data.SceneFilterTypeHolder
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FilterParcelTests {
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

        fun <T : Parcelable> parcelizeAndDeparcelize(obj: T): T? {
            val parcel = Parcel.obtain()
            parcel.writeParcelable(obj, 0)
            parcel.setDataPosition(0)
            return parcel.readParcelable(obj.javaClass.classLoader)
        }
    }

    @Test
    fun testSceneInt() {
        val filter =
            SceneFilterType(file_count = Optional.present(IntCriterionInput(100, Optional.absent(), CriterionModifier.INCLUDES_ALL)))
        val holder = SceneFilterTypeHolder(filter)
        val result = parcelizeAndDeparcelize(holder)
        Assert.assertNotNull(result)
        result!!
        Assert.assertNotNull(result.value)
        Assert.assertEquals(filter.file_count.getOrNull(), result.value?.file_count?.getOrNull())
        Assert.assertEquals(filter, result.value)
    }

    @Test
    fun testScene() {
        val filter =
            SceneFilterType(
                AND =
                    Optional.present(
                        SceneFilterType(
                            file_count =
                                Optional.present(
                                    IntCriterionInput(
                                        100,
                                        Optional.absent(),
                                        CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                title = Optional.present(StringCriterionInput("title", CriterionModifier.BETWEEN)),
                file_count =
                    Optional.present(
                        IntCriterionInput(
                            100,
                            Optional.absent(),
                            CriterionModifier.INCLUDES_ALL,
                        ),
                    ),
                phash_distance =
                    Optional.present(
                        PhashDistanceCriterionInput(
                            "distance",
                            CriterionModifier.IS_NULL,
                        ),
                    ),
                organized = Optional.present(true),
                resolution =
                    Optional.present(
                        ResolutionCriterionInput(
                            ResolutionEnum.HUGE,
                            CriterionModifier.NOT_EQUALS,
                        ),
                    ),
                orientation =
                    Optional.present(
                        OrientationCriterionInput(
                            listOf(
                                OrientationEnum.SQUARE,
                                OrientationEnum.PORTRAIT,
                            ),
                        ),
                    ),
                studios =
                    Optional.present(
                        HierarchicalMultiCriterionInput(
                            Optional.present(listOf("123")),
                            CriterionModifier.INCLUDES_ALL,
                            Optional.present(-1),
                            Optional.absent(),
                        ),
                    ),
                movies =
                    Optional.present(
                        MultiCriterionInput(
                            Optional.present(listOf("123")),
                            CriterionModifier.INCLUDES_ALL,
                            Optional.present(listOf("456")),
                        ),
                    ),
                date =
                    Optional.present(
                        DateCriterionInput(
                            "2024-01-01",
                            Optional.present("2024-01-02"),
                            CriterionModifier.EXCLUDES,
                        ),
                    ),
                created_at =
                    Optional.present(
                        TimestampCriterionInput(
                            "2024-01-01",
                            Optional.absent(),
                            CriterionModifier.NOT_MATCHES_REGEX,
                        ),
                    ),
            )
        val holder = SceneFilterTypeHolder(filter)
        val result = parcelizeAndDeparcelize(holder)
        Assert.assertNotNull(result)
        result!!
        Assert.assertNotNull(result.value)
        Assert.assertEquals(filter, result.value)
    }

//    @Test
//    fun testSceneFilter() {
//        val savedFilterData = getSavedFilterData("scene_savedfilter.json")
//        val sceneFilter =
//            FilterParser(Version.V0_25_0).convertSceneObjectFilter(savedFilterData.object_filter)
//        Assert.assertNotNull(sceneFilter!!)
//
//        val holder = SceneFilterTypeHolder(sceneFilter)
//        val result = parcelizeAndDeparcelize(holder)
//        Assert.assertEquals(sceneFilter, result)
//    }
}
