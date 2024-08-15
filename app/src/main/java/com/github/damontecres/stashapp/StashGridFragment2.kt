package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnChildLaidOutListener
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import com.github.damontecres.stashapp.views.TitleTransitionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StashGridFragment2<T : Query.Data, D : Any, C : Query.Data>(
    private val presenter: PresenterSelector = StashPresenter.SELECTOR,
    private val comparator: DiffUtil.ItemCallback<D>,
    val dataType: DataType,
    private var sortAndDirection: SortAndDirection = dataType.defaultSort,
    private val cardSize: Int? = null,
    val name: String? = null,
    private val factory: DataSupplierFactory<T, D, C>,
) : Fragment() {
    fun interface DataSupplierFactory<T : Query.Data, D : Any, C : Query.Data> {
        fun createDataSupplier(sortAndDirection: SortAndDirection): StashPagingSource.DataSupplier<T, D, C>
    }

    private lateinit var mAdapter: ObjectAdapter
    private lateinit var mGridPresenter: VerticalGridPresenter
    private lateinit var mGridViewHolder: VerticalGridPresenter.ViewHolder
    private var mOnItemViewSelectedListener: OnItemViewSelectedListener? = null

    var onItemViewClickedListener: OnItemViewClickedListener? = null
        set(value) {
            field = value
            mGridPresenter.onItemViewClickedListener = value
        }

    private var mSelectedPosition = -1

    var currentSelectedPosition: Int
        get() = mSelectedPosition
        set(position) {
            mSelectedPosition = position
            if (mGridViewHolder != null && mGridViewHolder.gridView.adapter != null) {
                mGridViewHolder.gridView.setSelectedPositionSmooth(position)
            }
        }

    private var titleTransitionHelper: TitleTransitionHelper? = null

    val currentSortAndDirection: SortAndDirection
        get() = sortAndDirection

    private lateinit var positionTextView: TextView
    private lateinit var totalCountTextView: TextView

    var requestFocus: Boolean = false

    private val mViewSelectedListener =
        OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            val position = mGridViewHolder.gridView.selectedPosition
            gridOnItemSelected(position)
            if (mOnItemViewSelectedListener != null) {
                mOnItemViewSelectedListener!!.onItemSelected(
                    itemViewHolder,
                    item,
                    rowViewHolder,
                    row,
                )
            }
        }

    private val mChildLaidOutListener =
        OnChildLaidOutListener { parent, view, position, id ->
            if (position == 0) {
                showOrHideTitle()
            }
        }

    private fun gridOnItemSelected(position: Int) {
        if (position != mSelectedPosition) {
            Log.v(TAG, "gridOnItemSelected=$position")
            mSelectedPosition = position
            showOrHideTitle()
        }
    }

    private fun showOrHideTitle() {
        if (mGridViewHolder.gridView.findViewHolderForAdapterPosition(mSelectedPosition) == null) {
            return
        }
//        if (!mGridViewHolder.gridView.hasPreviousViewInSameRow(mSelectedPosition)) {
//            showTitle(true)
//        } else {
//            showTitle(false)
//        }
        val shouldShowTitle = mSelectedPosition < mGridPresenter.numberOfColumns
        Log.v(
            TAG,
            "showOrHideTitle: mSelectedPosition=$mSelectedPosition, mGridPresenter.numberOfColumns=${mGridPresenter.numberOfColumns}",
        )
        showTitle(shouldShowTitle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gridPresenter = StashGridPresenter()
        val columns =
            cardSize ?: PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("cardSize", requireContext().getString(R.string.card_size_default))

        gridPresenter.numberOfColumns = columns
        setGridPresenter(gridPresenter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val root =
            inflater.inflate(
                androidx.leanback.R.layout.lb_vertical_grid_fragment,
                container,
                false,
            ) as ViewGroup
        val gridFrame = root.findViewById<View>(androidx.leanback.R.id.grid_frame) as ViewGroup

        val gridDock = root.findViewById<View>(androidx.leanback.R.id.browse_grid_dock) as ViewGroup
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock)
        gridDock.addView(mGridViewHolder.view)
        mGridViewHolder.gridView.setOnChildLaidOutListener(mChildLaidOutListener)

        return root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        positionTextView = view.findViewById(R.id.position_text)
        totalCountTextView = view.findViewById(R.id.total_count_text)

        onItemViewClickedListener = StashItemViewClickListener(requireContext())

        refresh(sortAndDirection)
    }

    private fun setGridPresenter(gridPresenter: VerticalGridPresenter) {
        mGridPresenter = gridPresenter
        mGridPresenter.onItemViewSelectedListener = mViewSelectedListener
        if (onItemViewClickedListener != null) {
            mGridPresenter.onItemViewClickedListener = onItemViewClickedListener
        }
    }

    private fun updateAdapter() {
        if (mGridViewHolder != null) {
            mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter)
            if (mSelectedPosition != -1) {
                mGridViewHolder.gridView.selectedPosition = mSelectedPosition
            }
        }
    }

    fun refresh(newSortAndDirection: SortAndDirection) {
        Log.v(TAG, "refresh: dataType=$dataType, newSortAndDirection=$newSortAndDirection")
        val pagingAdapter = PagingDataAdapter(presenter, comparator)
        mAdapter = pagingAdapter
        updateAdapter()
        val pageSize =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 50)
        val dataSupplier = factory.createDataSupplier(newSortAndDirection)
        val pagingSource =
            StashPagingSource(
                requireContext(),
                pageSize,
                dataSupplier = dataSupplier,
                useRandom = false,
                sortByOverride = null,
            )

//        val showFooter =
//            PreferenceManager.getDefaultSharedPreferences(requireContext())
//                .getBoolean(getString(R.string.pref_key_show_grid_footer), true)
//        val footerLayout = requireView().findViewById<View>(R.id.footer_layout)
//        if (showFooter) {
//            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
//                val count = pagingSource.getCount()
//                if (count > 0) {
//                    totalCountTextView.text = count.toString()
//                    footerLayout.animateToVisible()
//                }
//            }
//            setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
//                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
//                    val position =
//                        withContext(Dispatchers.IO) {
//                            val snapshot = pagingAdapter.snapshot()
//                            snapshot.indexOf(item) + 1
//                        }
//                    if (position > 0) {
//                        positionTextView.text = position.toString()
//                    }
//                }
//            }
//        } else {
//            footerLayout.visibility = View.GONE
//        }

        val flow =
            Pager(
                PagingConfig(
                    pageSize = pageSize,
                    prefetchDistance = pageSize * 2,
                    initialLoadSize = pageSize * 2,
                ),
            ) {
                pagingSource
            }.flow
                .cachedIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch(
            StashCoroutineExceptionHandler {
                Toast.makeText(
                    requireContext(),
                    "Error loading results: ${it.message}",
                    Toast.LENGTH_LONG,
                )
            },
        ) {
            flow.collectLatest {
                pagingAdapter.submitData(it)
            }
        }

        sortAndDirection = newSortAndDirection
    }

    override fun onStart() {
        super.onStart()

        val browseFrameLayout =
            requireView().findViewById<BrowseFrameLayout>(androidx.leanback.R.id.grid_frame)
        browseFrameLayout.onFocusSearchListener =
            BrowseFrameLayout.OnFocusSearchListener { focused: View?, direction: Int ->
                if (direction == View.FOCUS_UP) {
                    val filterButton = requireActivity().findViewById<View>(R.id.filter_button)
                    Log.v(TAG, "Found filterButton=$filterButton")
                    filterButton
                } else {
                    null
                }
            }
        if (requestFocus) {
            browseFrameLayout.requestFocus()
        }
    }

    fun setTitleView(titleView: View?) {
        if (view is ViewGroup && titleView != null) {
            titleTransitionHelper = TitleTransitionHelper(requireView() as ViewGroup, titleView)
        } else {
            titleTransitionHelper = null
        }
    }

    fun showTitle(show: Boolean) {
        titleTransitionHelper?.showTitle(show)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val TAG = "StashGridFragment2"
    }

    private class StashGridPresenter :
        VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false) {
        override fun initializeGridViewHolder(vh: ViewHolder?) {
            super.initializeGridViewHolder(vh)
            val gridView = vh!!.gridView
            val top = 10 // gridView.paddingTop
            val bottom = gridView.paddingBottom
            val right = 20
            val left = 20
            gridView.setPadding(left, top, right, bottom)
        }
    }
}
