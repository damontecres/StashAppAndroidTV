package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationOnItemViewClickedListener
import com.github.damontecres.stashapp.presenters.StashImageCardView
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.DefaultKeyEventCallback
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResultStatus
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.util.maybeStartPlayback
import com.github.damontecres.stashapp.util.showToastOnMain
import com.github.damontecres.stashapp.util.testStashConnection
import com.github.damontecres.stashapp.views.MainTitleView
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads a grid of cards with groups to browse.
 */
class MainFragment :
    BrowseSupportFragment(),
    DefaultKeyEventCallback {
    private val viewModel: ServerViewModel by activityViewModels()

    private val rowsAdapter = SparseArrayObjectAdapter(ListRowPresenter())
    private val adapters = ArrayList<ArrayObjectAdapter>()
    private val filterList = ArrayList<FilterArgs>()
    private lateinit var backCallback: OnBackPressedCallback

    private var currentPosition = Position(-1, -1)

    @Volatile
    private var fetchingData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED

        setupUIElements()
        setupEventListeners()

        adapter = rowsAdapter

        if (savedInstanceState == null) {
            Log.v(TAG, "Setting up observers")
            setupObservers()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        // Override the focus search so that pressing up from the rows will move to search first
        val browseFrameLayout =
            view.findViewById<BrowseFrameLayout>(androidx.leanback.R.id.browse_frame)
        browseFrameLayout.onFocusSearchListener =
            BrowseFrameLayout.OnFocusSearchListener { focused: View?, direction: Int ->
                if (direction == View.FOCUS_UP) {
                    requireActivity().findViewById(R.id.search_button)
                } else {
                    null
                }
            }
        if (!this::backCallback.isInitialized) {
            backCallback =
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, false) {
                    val pos = currentPosition
                    if (pos != null) {
                        if (pos.column > 0) {
                            selectedPosition = pos.row
                            (selectedRowViewHolder as ListRowPresenter.ViewHolder)
                                .gridView.selectedPosition = 0
                        } else if (pos.row > 0) {
                            selectedPosition = 0
                        }
                    }
                }
            (titleView as MainTitleView).focusListener.addListener { _, isFocused ->
                backCallback.isEnabled = !isFocused
            }
        }
        setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            val rowNum = rowsAdapter.indexOf(row)
            val col =
                (rowViewHolder as ListRowPresenter.ViewHolder).gridView.selectedPosition
            val pos = Position(rowNum, col)
//            Log.v(TAG, "$pos")
            backCallback.isEnabled = pos.row > 0 || pos.column > 0
            currentPosition = pos
        }
    }

    private fun setupObservers() {
        var firstTime = true
        viewModel.currentSettingsHash.observe(this) {
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler { ex ->
                    Toast.makeText(
                        requireContext(),
                        "Exception: ${ex.message}",
                        Toast.LENGTH_LONG,
                    )
                },
            ) {
                Log.d(TAG, "Settings hash changed: $it")
                if (!firstTime) {
                    clearData()
                    rowsAdapter.clear()

                    val server = viewModel.currentServer.value!!
                    server.updateServerPrefs()
                    val mainTitleView =
                        requireActivity().findViewById<MainTitleView>(R.id.browse_title_group)
                    mainTitleView.refreshMenuItems()
                    fetchData(server)
                }
                firstTime = false
            }
        }

        viewModel.currentServer.observe(this) { newServer ->
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler { ex ->
                    Toast.makeText(
                        requireContext(),
                        "Exception: ${ex.message}",
                        Toast.LENGTH_LONG,
                    )
                },
            ) {
                Log.d(TAG, "Server changed")
                if (newServer != null) {
                    val result =
                        testStashConnection(
                            requireContext(),
                            false,
                            StashClient.getApolloClient(newServer),
                        )
                    if (result.status == TestResultStatus.SUCCESS) {
                        newServer.updateServerPrefs()
                        val mainTitleView =
                            requireActivity().findViewById<MainTitleView>(R.id.browse_title_group)
                        mainTitleView.refreshMenuItems()
                        fetchData(newServer)
                    } else if (result.status == TestResultStatus.UNSUPPORTED_VERSION) {
                        clearData()
                        Log.w(
                            TAG,
                            "Server version is not supported: ${result.serverInfo?.version?.version}",
                        )
                        Toast
                            .makeText(
                                requireContext(),
                                "Server version ${result.serverInfo?.version?.version} is not supported!",
                                Toast.LENGTH_LONG,
                            ).show()
                    } else {
                        Log.w(TAG, "testStashConnection returned $result")
                        clearData()
                        requireActivity().findViewById<View?>(R.id.search_button).requestFocus()
                        Toast
                            .makeText(
                                requireContext(),
                                "Connection to Stash failed: ${result.status}",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                } else {
                    clearData()
                    Toast
                        .makeText(
                            requireContext(),
                            "Stash server not configured!",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        // over title
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(requireActivity(), R.color.fastlane_background)
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
            requireActivity().supportFragmentManager.commit {
                addToBackStack("search")
                replace(R.id.main_browse_fragment, StashSearchFragment())
            }
        }

        onItemViewClickedListener =
            NavigationOnItemViewClickedListener(viewModel.navigationManager) {
                val position = currentPosition
                if (position != null) {
                    val filter = filterList[position.row]
                    FilterAndPosition(filter, position.column)
                } else {
                    null
                }
            }
    }

    private fun clearData() {
        rowsAdapter.clear()
        adapters.forEach { it.clear() }
    }

    private fun fetchData(server: StashServer) {
        if (fetchingData) {
            return
        }
        fetchingData = true
        clearData()
        viewLifecycleOwner.lifecycleScope.launch(
            Dispatchers.IO +
                CoroutineExceptionHandler { _, ex ->
                    Log.e(TAG, "Exception in fetchData coroutine", ex)
                    Toast
                        .makeText(
                            requireContext(),
                            "Error fetching data: ${ex.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                },
        ) {
            try {
                val serverVersion = server.serverPreferences.serverVersion

                if (!Version.isStashVersionSupported(serverVersion)) {
                    val msg =
                        "Stash server version $serverVersion is not supported!"
                    Log.e(TAG, msg)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        clearData()
                        requireActivity().findViewById<View?>(R.id.search_button).requestFocus()
                    }
                } else {
                    try {
                        val queryEngine = QueryEngine(server)
                        val filterParser = FilterParser(serverVersion)

                        val config = queryEngine.getServerConfiguration()
                        server.serverPreferences.updatePreferences(config)

                        val ui = config.configuration.ui as Map<*, *>
                        val frontPageContent =
                            ui.getCaseInsensitive("frontPageContent") as List<Map<String, *>>?
                        if (frontPageContent == null) {
                            Toast
                                .makeText(
                                    requireContext(),
                                    "Unable to find front page content! Check the Web UI.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            return@launch
                        }
                        val pageSize =
                            PreferenceManager
                                .getDefaultSharedPreferences(requireContext())
                                .getInt(getString(R.string.pref_key_page_size), 25)
                        val frontPageParser =
                            FrontPageParser(requireContext(), queryEngine, filterParser, pageSize)
                        val jobs = frontPageParser.parse(frontPageContent)
                        jobs.forEachIndexed { index, job ->
                            job.await().let { row ->
                                if (row is FrontPageParser.FrontPageRow.Success) {
                                    filterList.add(row.filter)

                                    val adapter =
                                        ArrayObjectAdapter(StashPresenter.defaultClassPresenterSelector())
                                    adapter.addAll(0, row.data)
                                    adapter.add(row.filter)
                                    adapters.add(adapter)
                                    withContext(Dispatchers.Main) {
                                        rowsAdapter.set(
                                            index,
                                            ListRow(HeaderItem(row.name), adapter),
                                        )
                                    }
                                } else if (row is FrontPageParser.FrontPageRow.Error) {
                                    withContext(Dispatchers.Main) {
                                        Toast
                                            .makeText(
                                                requireContext(),
                                                "Error loading row $index on front page",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                }
                            }
                        }
                    } catch (ex: QueryEngine.StashNotConfiguredException) {
                        withContext(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    requireContext(),
                                    "Stash not configured. Please enter the URL in settings!",
                                    Toast.LENGTH_LONG,
                                ).show()
                            requireActivity().findViewById<View?>(R.id.search_button).requestFocus()
                        }
                    } catch (ex: QueryEngine.QueryException) {
                        withContext(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    requireContext(),
                                    ex.message,
                                    Toast.LENGTH_LONG,
                                ).show()
                            requireActivity().findViewById<View?>(R.id.search_button).requestFocus()
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Exception in fetchData", ex)
                showToastOnMain(
                    requireContext(),
                    "Error fetching data: ${ex.message}",
                    Toast.LENGTH_LONG,
                )
            } finally {
                fetchingData = false
            }
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            val item =
                currentPosition.let {
                    val row = rowsAdapter.get(it.row) as ListRow
                    row.adapter.get(it.column)
                }
            if (item != null && requireActivity().currentFocus is StashImageCardView) {
                maybeStartPlayback(requireContext(), item)
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    data class Position(
        val row: Int,
        val column: Int,
    )

    companion object {
        private const val TAG = "MainFragment"
    }
}
