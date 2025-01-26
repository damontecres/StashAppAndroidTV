package com.github.damontecres.stashapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.flip
import com.github.damontecres.stashapp.ui.Material3MainTheme

@Composable
fun SortByButton(
    dataType: DataType,
    current: SortAndDirection,
    onSortChange: (SortAndDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSort = current.sort
    val currentDirection = current.direction
    var sortByDropDown by remember { mutableStateOf(false) }
    val fontFamily = FontFamily(Font(resId = R.font.fa_solid_900))
    val context = LocalContext.current

    Box(modifier = modifier) {
        Button(
            onClick = { sortByDropDown = true },
            onLongClick = {
                onSortChange.invoke(SortAndDirection(currentSort, currentDirection.flip()))
            },
        ) {
            Text(
                text =
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = fontFamily)) {
                            append(
                                stringResource(
                                    if (currentDirection == SortDirectionEnum.ASC) {
                                        R.string.fa_caret_up
                                    } else {
                                        R.string.fa_caret_down
                                    },
                                ),
                            )
                        }
                        append(" ")
                        append(currentSort.getString(LocalContext.current))
                    },
            )
        }
        Material3MainTheme {
            DropdownMenu(
                expanded = sortByDropDown,
                onDismissRequest = { sortByDropDown = false },
            ) {
                dataType.sortOptions
                    .sortedBy { it.getString(context) }
                    .forEach { sortOption ->
                        DropdownMenuItem(
                            leadingIcon = {
                                if (sortOption == currentSort) {
                                    if (currentDirection == SortDirectionEnum.ASC) {
                                        Text(
                                            text = stringResource(R.string.fa_caret_up),
                                            fontFamily = fontFamily,
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.fa_caret_down),
                                            fontFamily = fontFamily,
                                        )
                                    }
                                }
                            },
                            text = { Text(sortOption.getString(context)) },
                            onClick = {
                                sortByDropDown = false
                                val newDirection =
                                    if (currentSort == sortOption) {
                                        currentDirection.flip()
                                    } else {
                                        currentDirection
                                    }
                                onSortChange.invoke(
                                    SortAndDirection(
                                        sortOption,
                                        newDirection,
                                    ),
                                )
                            },
                        )
                    }
            }
        }
    }
}
