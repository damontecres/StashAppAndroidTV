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
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
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

        fun <T : Parcelable> parcelizeAndDeparcelize(obj: T): T {
            val parcel = Parcel.obtain()
            parcel.writeParcelable(obj, 0)
            parcel.setDataPosition(0)
            return parcel.readParcelable(obj.javaClass.classLoader, obj.javaClass) as T
        }
    }

    @Test
    fun testSceneInt() {
        val filter =
            SceneFilterType(file_count = Optional.present(IntCriterionInput(100, Optional.absent(), CriterionModifier.INCLUDES_ALL)))
        val holder = SceneFilterTypeHolder(filter)
        val result = parcelizeAndDeparcelize(holder)
        Assert.assertNotNull(result)
        Assert.assertEquals(filter.file_count.getOrNull(), result.value.file_count.getOrNull())
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
