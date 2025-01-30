package com.github.damontecres.stashapp.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commitNow
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.DefaultKeyEventCallback
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.views.LoadingFragment
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    val frontPageRows = MutableLiveData<List<FrontPageParser.FrontPageRow.Success>>(listOf())
}

class ComposeMainFragment :
    Fragment(R.layout.compose_frame),
    DefaultKeyEventCallback {
    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: ServerViewModel by activityViewModels()

    private lateinit var backCallback: OnBackPressedCallback

    private var currentPosition: Position? = null
    private var dataFetchedFor: StashServer? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        composeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
//                MainTheme {
//                    val frontPageRows by mainViewModel.frontPageRows.observeAsState()
//                    val server: StashServer? by viewModel.currentServer.observeAsState()
//                    var showPopup by remember { mutableStateOf(false) }
//                    var itemLongClicked by remember { mutableStateOf<Any?>(null) }
//
//                    if (server == null) {
//                        // TODO
//                    } else {
//                        val server = server!!
//                        val queryEngine = QueryEngine(server)
//                        val filterParser = FilterParser(server.version)
//                        val frontPageContent =
//                            server.serverPreferences.uiConfiguration?.getCaseInsensitive("frontPageContent") as List<Map<String, *>>?
//                        if (frontPageContent != null) {
//                            Log.d(TAG, "${frontPageContent.size} front page rows")
//                            val pageSize = viewModel.cardUiSettings.value!!.maxSearchResults
//                            val frontPageParser =
//                                FrontPageParser(
//                                    requireContext(),
//                                    queryEngine,
//                                    filterParser,
//                                    pageSize,
//                                )
//                            LaunchedEffect(server) {
//                                val jobs = frontPageParser.parse(frontPageContent)
//                                mainViewModel.frontPageRows.value =
//                                    jobs.mapIndexedNotNull { index, job ->
//                                        job.await().let { row ->
//                                            if (row is FrontPageParser.FrontPageRow.Success) {
//                                                row
//                                            } else {
//                                                null
//                                            }
//                                        }
//                                    }
//                            }
//                        }
//                    }
//                    val longClicker =
//                        remember {
//                            LongClicker.default { item ->
//                                itemLongClicked = item
//                                showPopup = true
//                            }
//                        }
//                    if (!showPopup) {
//                        HomePage(
//                            modifier = Modifier.padding(16.dp),
//                            uiConfig = ComposeUiConfig(true),
//                            rows = frontPageRows ?: listOf(),
//                            itemOnClick = {
//                                StashApplication.navigationManager.navigate(
//                                    Destination.fromStashData(
//                                        it as StashData,
//                                    ),
//                                )
//                            },
//                            longClicker = longClicker,
//                        )
//                    } else {
//                        BackHandler {
//                            showPopup = false
//                        }
//                        Column(
//                            modifier =
//                                Modifier
//                                    .fillMaxHeight(),
//                            verticalArrangement = Arrangement.Center,
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                        ) {
//                            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
//                                Text(
//                                    text = extractTitle(itemLongClicked!! as StashData) ?: "",
//                                    modifier =
//                                        Modifier
//                                            .align(Alignment.CenterHorizontally),
//                                )
//                            }
//                            longClicker.getPopUpItems(itemLongClicked!!).forEach {
//                                ListItem(
//                                    modifier =
//                                        Modifier
//                                            .wrapContentWidth()
//                                            .align(Alignment.CenterHorizontally),
//                                    selected = false,
//                                    onClick = {
//                                        longClicker.onItemLongClick(itemLongClicked!!, it)
//                                    },
//                                    headlineContent = { Text(it.text) },
//                                )
//                            }
//                        }
//                    }
//                }
            }
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
//        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
//            val item =
//                currentPosition?.let {
//                    val row = rowsAdapter.get(it.row) as ListRow
//                    row.adapter.get(it.column)
//                }
//            if (item != null && requireActivity().currentFocus is StashImageCardView) {
//                maybeStartPlayback(requireContext(), item)
//            }
//        }
        return super.onKeyUp(keyCode, event)
    }

    private suspend fun showLoading() =
        withContext(Dispatchers.Main) {
            childFragmentManager.commitNow {
                add(requireView().id, LoadingFragment(), "stash_loading")
            }
        }

    private suspend fun hideLoading() =
        withContext(Dispatchers.Main) {
            childFragmentManager.findFragmentByTag("stash_loading")?.let {
                childFragmentManager.commitNow {
                    remove(it)
                }
            }
        }

    override fun onResume() {
        super.onResume()
        viewModel.updateServerPreferences()
    }

    data class Position(
        val row: Int,
        val column: Int,
    )

    companion object {
        private const val TAG = "MainFragment"
    }
}
