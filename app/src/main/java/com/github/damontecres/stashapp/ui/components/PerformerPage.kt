package com.github.damontecres.stashapp.ui.components

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.TabPage
import com.github.damontecres.stashapp.ui.TabProvider
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.ageInYears
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun PerformerPage(
    server: StashServer,
    id: String,
    itemOnClick: (Any) -> Unit,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    var performer by remember { mutableStateOf<PerformerData?>(null) }
    LaunchedEffect(id) {
        performer = QueryEngine(server).getPerformer(id)
    }
    val context = LocalContext.current

    fun createTab(
        dataType: DataType,
        initialFilter: FilterArgs,
    ): TabProvider =
        TabProvider(context.getString(dataType.pluralStringId)) { positionCallback ->
            StashGridControls(
                initialFilter = initialFilter,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                filterUiMode = FilterUiMode.CREATE_FILTER,
                modifier = Modifier,
                positionCallback = positionCallback,
            )
        }

    performer?.let { performer ->
        val performers =
            Optional.present(
                MultiCriterionInput(
                    value = Optional.present(listOf(performer.id)),
                    modifier = CriterionModifier.INCLUDES_ALL,
                ),
            )
        val tabs =
            listOf(
                TabProvider(stringResource(R.string.stashapp_details)) {
                    PerformerDetails(performer, Modifier.fillMaxSize())
                },
                createTab(
                    DataType.SCENE,
                    FilterArgs(
                        dataType = DataType.SCENE,
                        findFilter =
                            server!!
                                .serverPreferences
                                .getDefaultPageFilter(
                                    PageFilterKey.PERFORMER_SCENES,
                                ).findFilter,
                        objectFilter = SceneFilterType(performers = performers),
                    ),
                ),
                createTab(
                    DataType.GALLERY,
                    FilterArgs(
                        dataType = DataType.GALLERY,
                        findFilter =
                            server!!
                                .serverPreferences
                                .getDefaultPageFilter(
                                    PageFilterKey.PERFORMER_GALLERIES,
                                ).findFilter,
                        objectFilter = GalleryFilterType(performers = performers),
                    ),
                ),
                createTab(
                    DataType.IMAGE,
                    FilterArgs(
                        dataType = DataType.IMAGE,
                        findFilter =
                            server!!
                                .serverPreferences
                                .getDefaultPageFilter(
                                    PageFilterKey.PERFORMER_IMAGES,
                                ).findFilter,
                        objectFilter = ImageFilterType(performers = performers),
                    ),
                ),
                createTab(
                    DataType.GROUP,
                    FilterArgs(
                        dataType = DataType.GROUP,
                        findFilter =
                            server!!
                                .serverPreferences
                                .getDefaultPageFilter(
                                    PageFilterKey.PERFORMER_GROUPS,
                                ).findFilter,
                        objectFilter = GroupFilterType(performers = performers),
                    ),
                ),
            )
        val title =
            AnnotatedString
                .Builder()
                .apply {
                    withStyle(SpanStyle(color = Color.White, fontSize = 40.sp)) {
                        append(performer.name)
                    }
                    if (performer.disambiguation.isNotNullOrBlank()) {
                        withStyle(SpanStyle(color = Color.LightGray, fontSize = 24.sp)) {
                            append(" ")
                            append(performer.disambiguation)
                        }
                    }
                }.toAnnotatedString()
        TabPage(title, tabs, modifier)
    }
}

@Composable
fun PerformerDetails(
    perf: PerformerData,
    modifier: Modifier = Modifier,
) {
    val rows =
        buildList {
            if (perf.alias_list.isNotEmpty()) {
                add(
                    TableRow.from(
                        R.string.stashapp_aliases,
                        perf.alias_list.joinToString(", "),
                    ),
                )
            }
            if (!perf.birthdate.isNullOrBlank()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val age = perf.ageInYears.toString()
                    add(TableRow.from(R.string.stashapp_age, "$age (${perf.birthdate})"))
                }
            }
            add(TableRow.from(R.string.stashapp_death_date, perf.death_date))
            add(TableRow.from(R.string.stashapp_country, perf.country))
            add(TableRow.from(R.string.stashapp_ethnicity, perf.ethnicity))
            add(TableRow.from(R.string.stashapp_hair_color, perf.hair_color))
            add(TableRow.from(R.string.stashapp_eye_color, perf.eye_color))
            if (perf.height_cm != null) {
                val feet = floor(perf.height_cm / 30.48).toInt()
                val inches = (perf.height_cm / 2.54 - feet * 12).roundToInt()
                add(TableRow.from(R.string.stashapp_height, "${perf.height_cm} cm ($feet'$inches\")"))
            }
            if (perf.weight != null) {
                val pounds = (perf.weight * 2.2).roundToInt()
                add(TableRow.from(R.string.stashapp_weight, "${perf.weight} kg ($pounds lbs)"))
            }
            if (perf.penis_length != null) {
                val inches = kotlin.math.round(perf.penis_length / 2.54 * 100) / 100
                add(TableRow.from(R.string.stashapp_penis_length, "${perf.penis_length} cm ($inches\")"))
            }
            val circString =
                when (perf.circumcised) {
                    CircumisedEnum.CUT -> stringResource(R.string.stashapp_circumcised_types_CUT)
                    CircumisedEnum.UNCUT -> stringResource(R.string.stashapp_circumcised_types_UNCUT)
                    CircumisedEnum.UNKNOWN__, null -> null
                }
            add(TableRow.from(R.string.stashapp_circumcised, circString))

            add(TableRow.from(R.string.stashapp_tattoos, perf.tattoos))
            add(TableRow.from(R.string.stashapp_piercings, perf.piercings))
            add(TableRow.from(R.string.stashapp_career_length, perf.career_length))
            add(TableRow.from(R.string.stashapp_created_at, parseTimeToString(perf.created_at)))
            add(TableRow.from(R.string.stashapp_updated_at, parseTimeToString(perf.updated_at)))
            if (PreferenceManager
                    .getDefaultSharedPreferences(LocalContext.current)
                    .getBoolean(stringResource(R.string.pref_key_show_playback_debug_info), false)
            ) {
                add(TableRow.from(R.string.id, perf.id))
            }
        }.filterNotNull()
    DetailsPage(
        modifier = modifier,
        imageUrl = perf.image_path,
        tableRows = rows,
        favorite = perf.favorite,
        favoriteClick = {},
        rating100 = perf.rating100 ?: 0,
        rating100Click = {},
    )
}
