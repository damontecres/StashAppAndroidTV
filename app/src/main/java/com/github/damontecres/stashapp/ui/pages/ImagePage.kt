package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.ui.AppColors
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.MainTheme
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.playback.isDpad
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.showSetRatingToast
import kotlinx.coroutines.launch

private const val TAG = "ImagePage"

class ImageDetailsViewModel : ViewModel() {
    private var server: StashServer? = null

    val pager = MutableLiveData<ComposePager<ImageData>>()

    private val _image = MutableLiveData<ImageData>()
    val image: LiveData<ImageData> = _image

    val loadingState = MutableLiveData<ImageLoadingState>(ImageLoadingState.Loading)
    val tags = MutableLiveData<List<TagData>>(listOf())
    val performers = MutableLiveData<List<PerformerData>>(listOf())

    val rating100 = MutableLiveData(0)

    fun init(
        server: StashServer,
        filterArgs: FilterArgs,
        startPosition: Int,
    ): ImageDetailsViewModel {
        Log.v(TAG, "View model init")
        if (pager.value?.filter != filterArgs || server != this.server) {
            if (filterArgs.dataType != DataType.IMAGE) {
                throw IllegalArgumentException("Cannot use ${filterArgs.dataType}")
            }
            this.server = server
            val dataSupplierFactory = DataSupplierFactory(server.version)
            val dataSupplier =
                dataSupplierFactory.create<Query.Data, ImageData, Query.Data>(filterArgs)
            val pagingSource =
                StashPagingSource(QueryEngine(server), dataSupplier) { _, _, item -> item }
            val pager = ComposePager(filterArgs, pagingSource, viewModelScope)
            Log.v(TAG, "Pager created")
            viewModelScope.launch {
                pager.init()
                Log.v(TAG, "Pager size: ${pager.size()}")
                this@ImageDetailsViewModel.pager.value = pager
                updatePosition(startPosition)
            }
        }

        return this
    }

    fun updatePosition(position: Int) {
        pager.value?.let { pager ->
            viewModelScope.launch {
                try {
                    val image = pager.getBlocking(position)
                    Log.v(TAG, "Got image for $position: ${image != null}")
                    if (image != null) {
                        val queryEngine = QueryEngine(server!!)
                        rating100.value = image.rating100 ?: 0
                        _image.value = image

                        loadingState.value = ImageLoadingState.Success(image)
                        if (image.tags.isNotEmpty()) {
                            tags.value =
                                queryEngine.getTags(image.tags.map { it.id })
                            Log.v(TAG, "Got ${tags.value?.size} tags")
                        }
                        if (image.performers.isNotEmpty()) {
                            performers.value =
                                queryEngine.findPerformers(performerIds = image.performers.map { it.id })
                        }
                    } else {
                        loadingState.value = ImageLoadingState.Error
                    }
                } catch (ex: Exception) {
                    loadingState.value = ImageLoadingState.Error
                }
            }
        }
    }

    fun addTag(id: String) = mutateTags { add(id) }

    fun removeTag(id: String) = mutateTags { remove(id) }

    private fun mutateTags(mutator: MutableList<String>.() -> Unit) {
        val ids = tags.value?.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                TODO()
            }
        }
    }

    fun updateRating(
        imageId: String,
        rating100: Int,
    ) {
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val mutationEngine = MutationEngine(server!!)
            val newRating =
                mutationEngine.updateImage(imageId, rating100 = rating100)?.rating100 ?: 0
            this@ImageDetailsViewModel.rating100.value = newRating
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }

    companion object {
//        val SERVER_KEY = object : CreationExtras.Key<StashServer> {}
//        val FILTER_KEY = object : CreationExtras.Key<FilterArgs> {}
//        val POSITION_KEY = object : CreationExtras.Key<Int> {}
//        val Factory: ViewModelProvider.Factory =
//            viewModelFactory {
//                initializer {
//                    val server = this[SERVER_KEY]!!
//                    val filter = this[FILTER_KEY]!!
//                    val position = this[POSITION_KEY]!!
//                    ImageDetailsViewModel(server, filter, position).init()
//                }
//            }
    }
}

sealed class ImageLoadingState {
    data object Loading : ImageLoadingState()

    data object Error : ImageLoadingState()

    data class Success(
        val image: ImageData,
    ) : ImageLoadingState()
}

@Composable
fun ImagePage(
    server: StashServer,
    navigationManager: NavigationManagerCompose,
    filter: FilterArgs,
    startPosition: Int,
    startSlideshow: Boolean,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    viewModel: ImageDetailsViewModel = viewModel(),
) {
    LaunchedEffect(server, filter) {
        viewModel.init(server, filter, startPosition)
    }

    val imageState by viewModel.image.observeAsState()
    val tags by viewModel.tags.observeAsState(listOf())
    val performers by viewModel.performers.observeAsState(listOf())

    var zoomFactor by rememberSaveable { mutableFloatStateOf(1f) }
    var rotation by rememberSaveable { mutableIntStateOf(0) }
    var showOverlay by rememberSaveable { mutableStateOf(false) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }

    val rotateAnimation: Float by animateFloatAsState(
        targetValue = rotation.toFloat(),
        label = "image_rotation",
    )
    val zoomAnimation: Float by animateFloatAsState(
        targetValue = zoomFactor,
        label = "image_zoom",
    )
    val panXAnimation: Float by animateFloatAsState(
        targetValue = panX,
        label = "image_panX",
    )
    val panYAnimation: Float by animateFloatAsState(
        targetValue = panY,
        label = "image_panY",
    )

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val density = LocalDensity.current

    fun reset(resetRotate: Boolean) {
        zoomFactor = 1f
        panX = 0f
        panY = 0f
        if (resetRotate) rotation = 0
    }

    Box(
        modifier =
            modifier
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    var result = false
                    if (it.type != KeyEventType.KeyUp) {
                        result = false
                    } else if (!showOverlay && zoomFactor * 100 > 105 && isDpad(it)) {
                        // Image is zoomed in
                        when (it.key) {
                            Key.DirectionLeft -> panX += with(density) { 30.dp.toPx() }
                            Key.DirectionRight -> panX -= with(density) { 30.dp.toPx() }
                            Key.DirectionUp -> panY += with(density) { 30.dp.toPx() }
                            Key.DirectionDown -> panY -= with(density) { 30.dp.toPx() }
                        }
                        result = true
                    } else if (!showOverlay && zoomFactor * 100 > 105 && it.key == Key.Back) {
                        reset(false)
                        result = true
                    } else if (showOverlay && it.key == Key.Back) {
                        showOverlay = false
                        result = true
                    } else if (!showOverlay && it.key != Key.Back) {
                        showOverlay = true
                        result = true
                    }
                    result
                },
    ) {
        imageState?.let { image ->
            if (image.paths.image.isNotNullOrBlank()) {
                AsyncImage(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomAnimation
                                scaleY = zoomAnimation
                                translationX = panXAnimation
                                translationY = panYAnimation
                            }.rotate(rotateAnimation),
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(image.paths.image)
                            .crossfade(true)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                )
            }
            if (showOverlay) {
                ImageOverlay(
                    image = image,
                    tags = tags,
                    performers = performers,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    onZoom = { zoomFactor = (zoomFactor + it).coerceIn(1f, 5f) },
                    onRotate = { rotation += it },
                    onReset = { reset(true) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun ImageOverlay(
    image: ImageData,
    tags: List<TagData>,
    performers: List<PerformerData>,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    onZoom: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ImageControlsOverlay(
            onZoom = onZoom,
            onRotate = onRotate,
            onReset = onReset,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun ImageControlsOverlay(
    onZoom: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Row(
        modifier =
            modifier
                .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ImageControlButton(
            stringRes = R.string.fa_rotate_left,
            onClick = { onRotate(-90) },
            modifier =
                Modifier
                    .focusRequester(focusRequester),
        )
        ImageControlButton(
            stringRes = R.string.fa_rotate_right,
            onClick = { onRotate(90) },
        )
        ImageControlButton(
            stringRes = R.string.fa_magnifying_glass_plus,
            onClick = { onZoom(.15f) },
        )
        ImageControlButton(
            stringRes = R.string.fa_magnifying_glass_minus,
            onClick = { onZoom(-.15f) },
        )
        ImageControlButton(
            drawableRes = R.drawable.baseline_undo_24,
            onClick = onReset,
        )
    }
}

@Composable
fun ImageControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes stringRes: Int = 0,
    @DrawableRes drawableRes: Int = 0,
) {
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(CircleShape),
        colors =
            ButtonDefaults.colors(
                containerColor = AppColors.TransparentBlack25,
                focusedContainerColor = MaterialTheme.colorScheme.border,
            ),
        contentPadding = PaddingValues(8.dp),
        modifier =
            modifier
                .padding(8.dp)
                .size(56.dp, 56.dp),
    ) {
        if (stringRes != 0) {
            Text(
                text = stringResource(stringRes),
                fontFamily = FontAwesome,
                fontSize = 32.sp,
            )
        } else {
            Icon(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(drawableRes),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview
@Composable
private fun ImageControlsOverlayPreview() {
    MainTheme {
        ImageControlsOverlay(
            onZoom = {},
            onRotate = {},
            onReset = {},
        )
    }
}
