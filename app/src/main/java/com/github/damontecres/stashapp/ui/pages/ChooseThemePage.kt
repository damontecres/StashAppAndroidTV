package com.github.damontecres.stashapp.ui.pages

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.IconRowText
import com.github.damontecres.stashapp.ui.cards.ImageOverlay
import com.github.damontecres.stashapp.ui.cards.RootCard
import com.github.damontecres.stashapp.ui.cards.dataTypeImageHeight
import com.github.damontecres.stashapp.ui.cards.dataTypeImageWidth
import com.github.damontecres.stashapp.ui.chooseColorScheme
import com.github.damontecres.stashapp.ui.defaultColorSchemeSet
import com.github.damontecres.stashapp.ui.enableMarquee
import com.github.damontecres.stashapp.ui.parseThemeJson
import com.github.damontecres.stashapp.ui.readThemeJson
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.durationToString
import java.io.File
import java.util.EnumMap

private const val TAG = "ChooseTheme"

@Composable
fun ChooseThemePage(
    server: StashServer,
    navigationManager: NavigationManager,
    uiConfig: ComposeUiConfig,
    onChooseTheme: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isSystemInDark = isSystemInDarkTheme()
    val currentColorScheme = MaterialTheme.colorScheme
    var colorScheme by remember { mutableStateOf(currentColorScheme) }
    var name by remember {
        mutableStateOf(
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_key_ui_theme_file), null),
        )
    }
    var json by remember { mutableStateOf<String?>(null) }

    var themes by remember { mutableStateOf(getThemes(context)) }

    var showDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.background(Color.Black),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            MaterialTheme(colorScheme = colorScheme) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .background(colorScheme.background)
                            .border(2.dp, color = Color.LightGray),
                    contentAlignment = Alignment.Center,
                ) {
                    val dataTypeMap = EnumMap<DataType, Int>(DataType::class.java)
                    dataTypeMap[DataType.TAG] = 2
                    dataTypeMap[DataType.PERFORMER] = 3
                    dataTypeMap[DataType.GROUP] = 2
                    dataTypeMap[DataType.MARKER] = 5
                    RootCard(
                        modifier = Modifier.padding(32.dp),
                        item = "",
                        onClick = {},
                        title = "Sample Title",
                        imageContent = {
                            Image(
                                modifier = Modifier.fillMaxSize(.75f),
                                painter = painterResource(R.drawable.baseline_camera_indoor_48),
                                contentDescription = null,
                            )
                        },
                        subtitle = {
                            Text(
                                text = "This is the description",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        description = {
                            IconRowText(
                                dataTypeMap,
                                2,
                                Modifier
                                    .enableMarquee(it)
                                    .align(Alignment.Center),
                            )
                        },
                        uiConfig = uiConfig,
                        imageWidth = dataTypeImageWidth(DataType.SCENE).dp / 1.5f,
                        imageHeight = dataTypeImageHeight(DataType.SCENE).dp / 1.5f,
                        longClicker = { _: Any, _ -> },
                        getFilterAndPosition = null,
                        imageOverlay = {
                            ImageOverlay(uiConfig.ratingAsStars, rating100 = 80) {
                                val duration = durationToString(1625.2)
                                Text(
                                    modifier =
                                        Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp),
                                    text = duration,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    modifier =
                                        Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    text = "1080p",
                                )
                                val percentWatched = .6
                                Box(
                                    modifier =
                                        Modifier
                                            .align(Alignment.BottomStart)
                                            .background(
                                                MaterialTheme.colorScheme.tertiary, // TODO?
                                            ).clip(RectangleShape)
                                            .height(4.dp)
                                            .width((ScenePresenter.CARD_WIDTH * percentWatched).dp / 2),
                                )
                            }
                        },
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .edit {
                            putString(context.getString(R.string.pref_key_ui_theme_file), name)
                        }
                    onChooseTheme.invoke(name)
                    navigationManager.goToMain()
                },
            ) {
                Text(
                    text = "Save",
                )
            }
        }
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Button(
                        onClick = {
                            name = null
                            colorScheme = chooseColorScheme(context, isSystemInDark, defaultColorSchemeSet).tvColorScheme
                        },
                    ) {
                        Text(
                            text = "Use default",
                        )
                    }
                }
                item {
                    Button(
                        onClick = { showDialog = true },
                    ) {
                        Text(
                            text = "Search for a tag",
                        )
                    }
                }
                item {
                    Spacer(Modifier.width(16.dp))
                }
                items(themes) {
                    Button(
                        onClick = {
                            name = it
                            val theme = readThemeJson(context, it)
                            colorScheme =
                                chooseColorScheme(context, isSystemInDark, theme).tvColorScheme
                        },
                        onLongClick = {
                            if (deleteJson(context, it)) {
                                themes = getThemes(context)
                            }
                        },
                    ) {
                        Text(
                            text = it,
                        )
                    }
                }
            }
        }
    }
    SearchForDialog(
        show = showDialog,
        startingSearchQuery = "Theme",
        dialogTitle = "Choose tag to import theme",
        dataType = DataType.TAG,
        uiConfig = uiConfig,
        onDismissRequest = { showDialog = false },
        onItemClick = { item ->
            item as TagData
            if (item.description.isNotNullOrBlank()) {
                val colorSchemeSet =
                    try {
                        parseThemeJson(item.description)
                    } catch (ex: Exception) {
                        Log.w(TAG, "Cannot parse JSON", ex)
                        Toast
                            .makeText(
                                context,
                                "Could not parse JSON: ${ex.localizedMessage}",
                                Toast.LENGTH_LONG,
                            ).show()
                        return@SearchForDialog
                    }
                try {
                    writeJson(context, item.name, item.description)
                } catch (ex: Exception) {
                    Log.w(TAG, "Cannot save JSON file", ex)
                    Toast
                        .makeText(
                            context,
                            "Could not save file: ${ex.localizedMessage}",
                            Toast.LENGTH_LONG,
                        ).show()
                    return@SearchForDialog
                }
                name = item.name
                json = item.description
                colorScheme = chooseColorScheme(context, isSystemInDark, colorSchemeSet).tvColorScheme
                themes = getThemes(context)
            } else {
                Toast
                    .makeText(context, "Tag description is empty.", Toast.LENGTH_SHORT)
                    .show()
            }
        },
        showRecent = false,
        showSuggestions = false,
        allowCreate = false,
    )
}

fun writeJson(
    context: Context,
    name: String,
    content: String,
) {
    val dir = context.getDir("themes", Context.MODE_PRIVATE)
    File(dir, "$name.json").writeText(content)
}

fun getThemes(context: Context): List<String> {
    val dir = context.getDir("themes", Context.MODE_PRIVATE)
    return dir.listFiles { _, name -> name.endsWith(".json") }?.map { it.name.removeSuffix(".json") } ?: listOf()
}

fun deleteJson(
    context: Context,
    name: String,
): Boolean {
    val dir = context.getDir("themes", Context.MODE_PRIVATE)
    return File(dir, "$name.json").delete()
}
