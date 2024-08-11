package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.data.StashFilter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.TestResultStatus
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.util.showToastOnMain
import com.github.damontecres.stashapp.util.testStashConnection
import com.github.damontecres.stashapp.views.ClassOnItemViewClickedListener
import com.github.damontecres.stashapp.views.MainTitleView
import com.github.damontecres.stashapp.views.OnImageFilterClickedListener
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment() {
    private val viewModel: ServerViewModel by activityViewModels()

    private val rowsAdapter = SparseArrayObjectAdapter(ListRowPresenter())
    private val adapters = ArrayList<ArrayObjectAdapter>()
    private val filterList = SparseArray<StashFilter>()
    private lateinit var mBackgroundManager: BackgroundManager

    @Volatile
    private var fetchingData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED

        prepareBackgroundManager()

        setupUIElements()

        setupEventListeners()

        adapter = rowsAdapter

        viewModel.recomputeSettingsHash()
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
        if (savedInstanceState == null) {
            setupObservers()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.recomputeSettingsHash()
    }

    private fun setupObservers() {
        var firstTime = true
        viewModel.currentSettingsHash.observe(viewLifecycleOwner) {
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler { ex ->
                    Toast.makeText(
                        requireContext(),
                        "Exception: ${ex.message}",
                        Toast.LENGTH_LONG,
                    )
                },
            ) {
                if (!firstTime) {
                    clearData()
                    rowsAdapter.clear()

                    val serverPrefs = ServerPreferences(requireContext())
                    serverPrefs.updatePreferences()
                    val mainTitleView =
                        requireActivity().findViewById<MainTitleView>(R.id.browse_title_group)
                    mainTitleView.refreshMenuItems()
                    fetchData(serverPrefs.serverVersion)
                }
                firstTime = false
            }
        }

        viewModel.currentServer.observe(viewLifecycleOwner) {
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
                try {
                    val result =
                        testStashConnection(
                            requireContext(),
                            false,
                            StashClient.getApolloClient(requireContext()),
                        )
                    if (result.status == TestResultStatus.SUCCESS) {
                        val serverInfo = result.serverInfo!!
                        ServerPreferences(requireContext()).updatePreferences()
                        val mainTitleView =
                            requireActivity().findViewById<MainTitleView>(R.id.browse_title_group)
                        mainTitleView.refreshMenuItems()
                        fetchData(Version.tryFromString(serverInfo.version.version))
                    } else {
                        clearData()
                        requireActivity().findViewById<View?>(R.id.search_button).requestFocus()
                        Toast.makeText(
                            requireContext(),
                            "Connection to Stash failed.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                } catch (ex: QueryEngine.StashNotConfiguredException) {
                    clearData()
                    Toast.makeText(
                        requireContext(),
                        "Stash server not configured!",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
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
//            Toast.makeText(activity!!, "Implement your own in-app search", Toast.LENGTH_LONG)
//                .show()
            requireActivity().startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        onItemViewClickedListener =
            ClassOnItemViewClickedListener(StashItemViewClickListener(requireActivity()))
                .addListenerForClass(
                    ImageData::class.java,
                    OnImageFilterClickedListener(requireContext()) { image: ImageData ->
                        val position = getCurrentPosition()
                        if (position != null) {
                            val filter = filterList.get(position.row)
                            if (filter != null) {
                                return@OnImageFilterClickedListener OnImageFilterClickedListener.FilterPosition(
                                    filter,
                                    position.column,
                                )
                            }
                        }
                        OnImageFilterClickedListener.FilterPosition(null, null)
                    },
                )
    }

    private fun clearData() {
        rowsAdapter.clear()
        adapters.forEach { it.clear() }
    }

    private fun fetchData(serverVersion: Version?) {
        if (fetchingData) {
            return
        }
        fetchingData = true
        clearData()
        viewLifecycleOwner.lifecycleScope.launch(
            Dispatchers.IO +
                CoroutineExceptionHandler { _, ex ->
                    Log.e(TAG, "Exception in fetchData coroutine", ex)
                    Toast.makeText(
                        requireContext(),
                        "Error fetching data: ${ex.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                },
        ) {
            try {
                if (serverVersion == null) {
                    Log.w(TAG, "Version returned by server is null")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Could not determine the server version. Things may not work!",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                if (serverVersion != null &&
                    !Version.isStashVersionSupported(serverVersion)
                ) {
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
                        val queryEngine = QueryEngine(requireContext(), showToasts = true)
                        val filterParser =
                            FilterParser(serverVersion ?: Version.MINIMUM_STASH_VERSION)

                        val config = queryEngine.getServerConfiguration()
                        ServerPreferences(requireContext()).updatePreferences(config)

                        val ui = config.configuration.ui
                        val frontPageContent =
                            (ui as Map<String, *>).getCaseInsensitive("frontPageContent") as List<Map<String, *>>
                        val pageSize =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .getInt(getString(R.string.pref_key_page_size), 25)
                        val frontPageParser =
                            FrontPageParser(queryEngine, filterParser, pageSize)
                        val jobs = frontPageParser.parse(frontPageContent)
                        jobs.forEachIndexed { index, job ->
                            job.await().let { row ->
                                if (row.successful) {
                                    val rowData = row.data!!
                                    filterList.set(index, rowData.filter)

                                    val adapter = ArrayObjectAdapter(StashPresenter.SELECTOR)
                                    adapter.addAll(0, rowData.data)
                                    adapter.add(rowData.filter)
                                    adapters.add(adapter)
                                    withContext(Dispatchers.Main) {
                                        rowsAdapter.set(
                                            index,
                                            ListRow(HeaderItem(rowData.name), adapter),
                                        )
                                    }
                                } else if (row.result == FrontPageParser.FrontPageRowResult.ERROR) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
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
                            Toast.makeText(
                                requireContext(),
                                "Stash not configured. Please enter the URL in settings!",
                                Toast.LENGTH_LONG,
                            ).show()
                            requireActivity().findViewById<View?>(R.id.search_button).requestFocus()
                        }
                    } catch (ex: QueryEngine.QueryException) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "Query error: ${ex.message}",
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

    private fun getCurrentPosition(): Position? {
        val rowPos = selectedPosition
        if (rowPos >= 0 && selectedRowViewHolder != null) {
            val columnPos =
                (selectedRowViewHolder as ListRowPresenter.ViewHolder).gridView.selectedPosition
            if (columnPos >= 0) {
                Log.v(TAG, "row=$rowPos, column=$columnPos")
                return Position(rowPos, columnPos)
            }
        }
        return null
    }

    /**
     * Return true if back was handled
     */
    fun onBackPressed(): Boolean {
        val pos = getCurrentPosition()
        if (pos != null) {
            if (pos.column > 0) {
                selectedPosition = pos.row
                (selectedRowViewHolder as ListRowPresenter.ViewHolder).gridView.selectedPosition = 0
                return true
            } else if (pos.row > 0) {
                selectedPosition = 0
                return true
            }
        }
        return false
    }

    data class Position(val row: Int, val column: Int)

    companion object {
        private const val TAG = "MainFragment"
    }
}
