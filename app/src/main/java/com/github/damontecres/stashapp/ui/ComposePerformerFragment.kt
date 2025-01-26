package com.github.damontecres.stashapp.ui

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.viewModels
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.databinding.PerformerDetailsComposeBinding
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.models.PerformerViewModel

class ComposePerformerFragment : ComposeTabFragment() {
    private val viewModel: PerformerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.init(requireArguments())
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val perf by viewModel.item.observeAsState()
        val server by serverViewModel.currentServer.observeAsState()
        if (perf != null && server != null) {
            val performer = perf!!
            val performers =
                Optional.present(
                    MultiCriterionInput(
                        value = Optional.present(listOf(performer.id)),
                        modifier = CriterionModifier.INCLUDES_ALL,
                    ),
                )
            val tabs =
                listOf(
                    TabProvider(getString(R.string.stashapp_details)) {
                        AndroidViewBinding(
                            PerformerDetailsComposeBinding::inflate,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    createTab(
                        DataType.SCENE,
                        FilterArgs(
                            dataType = DataType.SCENE,
                            findFilter = server!!.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_SCENES).findFilter,
                            objectFilter = SceneFilterType(performers = performers),
                        ),
                    ),
                    createTab(
                        DataType.GALLERY,
                        FilterArgs(
                            dataType = DataType.GALLERY,
                            findFilter = server!!.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_GALLERIES).findFilter,
                            objectFilter = GalleryFilterType(performers = performers),
                        ),
                    ),
                    createTab(
                        DataType.IMAGE,
                        FilterArgs(
                            dataType = DataType.IMAGE,
                            findFilter = server!!.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_IMAGES).findFilter,
                            objectFilter = ImageFilterType(performers = performers),
                        ),
                    ),
                    createTab(
                        DataType.GROUP,
                        FilterArgs(
                            dataType = DataType.GROUP,
                            findFilter = server!!.serverPreferences.getDefaultFilter(PageFilterKey.PERFORMER_GROUPS).findFilter,
                            objectFilter = GroupFilterType(performers = performers),
                        ),
                    ),
                )
            val title =
                AnnotatedString
                    .Builder()
                    .apply {
                        withStyle(SpanStyle(color = Color.White, fontSize = 24.sp)) {
                            append(performer.name)
                        }
                        if (performer.disambiguation.isNotNullOrBlank()) {
                            withStyle(SpanStyle(color = Color.LightGray, fontSize = 18.sp)) {
                                append(" (")
                                append(performer.disambiguation)
                                append(")")
                            }
                        }
                    }.toAnnotatedString()
            TabPage(title, tabs, modifier)
        }
    }
}
