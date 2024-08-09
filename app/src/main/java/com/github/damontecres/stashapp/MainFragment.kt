package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
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
import com.github.damontecres.stashapp.util.asSlimeSceneData
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.showToastOnMain
import com.github.damontecres.stashapp.util.testStashConnection
import com.github.damontecres.stashapp.views.ClassOnItemViewClickedListener
import com.github.damontecres.stashapp.views.MainTitleView
import com.github.damontecres.stashapp.views.OnImageFilterClickedListener
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Objects

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment() {
    private val rowsAdapter = SparseArrayObjectAdapter(ListRowPresenter())
    private val adapters = ArrayList<ArrayObjectAdapter>()
    private val filterList = SparseArray<StashFilter>()
    private lateinit var mBackgroundManager: BackgroundManager
    private var serverHash: Int? = null

    @Volatile
    private var fetchingData = false

    /**
     * This just hashes a few preferences that affect what this fragment shows
     */
    private fun computeServerHash(): Int {
        val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val url = manager.getString("stashUrl", null)
        val apiKey = manager.getString("stashApiKey", null)
        val maxSearchResults = manager.getInt("maxSearchResults", 0)
        val playVideoPreviews = manager.getBoolean("playVideoPreviews", true)
        val columns = manager.getInt("cardSize", getString(R.string.card_size_default))
        val showRatings = manager.getBoolean(getString(R.string.pref_key_show_rating), true)
        return Objects.hash(url, apiKey, maxSearchResults, playVideoPreviews, columns, showRatings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serverHash = computeServerHash()

        headersState = HEADERS_DISABLED

        prepareBackgroundManager()

        setupUIElements()

        setupEventListeners()

        adapter = rowsAdapter
        if (savedInstanceState == null) {
            requireActivity().supportFragmentManager.addOnBackStackChangedListener {
                if (requireActivity().supportFragmentManager.backStackEntryCount == 0) {
                    doOnResume()
                }
            }
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
        if (savedInstanceState == null) {
            doOnResume()
        }
    }

    override fun onResume() {
        super.onResume()
        doOnResume()
    }

    private fun doOnResume() {
        val newServerHash = computeServerHash()
        if (serverHash != newServerHash) {
            Log.v(TAG, "server hash changed")
            clearData()
            rowsAdapter.clear()
        }
        serverHash = newServerHash

        viewLifecycleOwner.lifecycleScope.launch(
            StashCoroutineExceptionHandler { ex ->
                Toast.makeText(
                    requireContext(),
                    "Exception: ${ex.message}",
                    Toast.LENGTH_LONG,
                )
            },
        ) {
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
                    if (rowsAdapter.size() == 0) {
                        fetchData(serverInfo)
                    }
                    try {
                        val position = getCurrentPosition()
                        if (position != null) {
                            val adapter = adapters[position.row]
                            val item = adapter.get(position.column)
                            if (item is SlimSceneData) {
                                val queryEngine = QueryEngine(requireContext())
                                queryEngine.getScene(item.id)?.let {
                                    adapter.replace(position.column, it.asSlimeSceneData)
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Exception", ex)
                    }
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

    private fun fetchData(serverInfo: ServerInfoQuery.Data) {
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
                if (serverInfo.version.version == null) {
                    Log.w(TAG, "Version returned by server is null")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Could not determine the server version. Things may not work!",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                if (serverInfo.version.version != null &&
                    !Version.isStashVersionSupported(
                        Version.fromString(
                            serverInfo.version.version,
                        ),
                    )
                ) {
                    val msg =
                        "Stash server version ${serverInfo.version.version} is not supported!"
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
                            FilterParser(
                                Version.tryFromString(serverInfo.version.version)
                                    ?: Version.MINIMUM_STASH_VERSION,
                            )

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
