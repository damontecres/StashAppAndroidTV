package com.github.damontecres.stashapp.ui.components.prefs

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.JobQueueQuery
import com.github.damontecres.stashapp.api.fragment.StashJob
import com.github.damontecres.stashapp.api.type.JobStatusUpdateType
import com.github.damontecres.stashapp.di.server.MutationEngine
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.server.SubscriptionEngine
import com.github.damontecres.stashapp.di.services.InterfaceService
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.ui.indexOfFirstOrNull
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.launchDefault
import com.github.damontecres.stashapp.util.plugin.CompanionPluginService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class PreferencesViewModel(
    private val context: Application,
    private val queryEngine: QueryEngine,
    private val mutationEngine: MutationEngine,
    private val subscriptionEngine: SubscriptionEngine,
    private val companionPluginService: CompanionPluginService,
    val navigationManager: NavigationManager,
    val serverRepository: ServerRepository,
    val updateChecker: UpdateChecker,
    private val interfaceService: InterfaceService,
) : ViewModel() {
    private val lock = Mutex()

    private val _state = MutableStateFlow(PreferencesPageState())
    val state: StateFlow<PreferencesPageState> = _state

    fun init() {
        interfaceService.setTitle(context.getString(R.string.preferences))
        _state.update { it.copy(runningJobs = emptyList()) }
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val runningJobs =
                queryEngine
                    .executeQuery(JobQueueQuery())
                    .data
                    ?.jobQueue
                    ?.map { it.stashJob }
                    .orEmpty()
            _state.update { it.copy(runningJobs = runningJobs) }

            subscriptionEngine.subscribeToJobs { update ->
                val type = update.jobsSubscribe.type
                val jobData = update.jobsSubscribe.job.stashJob
                viewModelScope.launch(StashCoroutineExceptionHandler()) {
                    handleUpdate(type, jobData)
                }
            }
        }
        updateCacheUsage()
    }

    private suspend fun handleUpdate(
        type: JobStatusUpdateType,
        jobData: StashJob,
    ) {
        lock.withLock {
            val mutable = state.value.runningJobs.toMutableList()
            val index = mutable.indexOfFirstOrNull { it.id == jobData.id }
            // Timer for removing?
            when (type) {
                JobStatusUpdateType.ADD -> {
                    mutable.add(jobData)
                }

                JobStatusUpdateType.REMOVE -> {
                    if (index != null && index >= 0) {
                        mutable[index] = jobData
                    }
                    viewModelScope.launch(StashCoroutineExceptionHandler()) {
                        delay(10_000)
                        lock.withLock {
                            val mutable = state.value.runningJobs.toMutableList()
                            if (mutable.removeIf { it.id == jobData.id }) {
                                _state.update { it.copy(runningJobs = mutable) }
                            }
                        }
                    }
                }

                JobStatusUpdateType.UPDATE -> {
                    if (index != null && index >= 0) {
                        mutable[index] = jobData
                    }
                }

                JobStatusUpdateType.UNKNOWN__ -> {
                    Log.w(TAG, "Unknown job update type for $jobData")
                }
            }
            _state.update { it.copy(runningJobs = mutable) }
        }
    }

    fun updateCacheUsage() {
        val networkDisk = Constants.getNetworkCache(context).size()
        val imageUsedMemory = context.imageLoader.memoryCache?.size ?: 0L
        val imageMaxMemory = context.imageLoader.memoryCache?.maxSize ?: 0L
        val imageDisk = context.imageLoader.diskCache?.size ?: 0L
        _state.update {
            it.copy(
                cacheUsage =
                    CacheUsage(
                        networkDiskUsed = networkDisk,
                        imageMemoryUsed = imageUsedMemory,
                        imageMemoryMax = imageMaxMemory,
                        imageDiskUsed = imageDisk,
                    ),
            )
        }
    }

    fun onTriggerScan() {
        viewModelScope.launchDefault {
            // TODO track status
            mutationEngine.triggerScan(serverRepository.currentServer.value.serverPreferences)
        }
    }

    fun onTriggerGenerate() {
        viewModelScope.launchDefault {
            // TODO track status
            mutationEngine.triggerGenerate(serverRepository.currentServer.value.serverPreferences)
        }
    }

    fun onSendLogs(verbose: Boolean) {
        viewModelScope.launchDefault {
            companionPluginService.sendLogCat(verbose)
        }
    }

    companion object {
        private const val TAG = "PreferencesViewModel"
    }
}

data class CacheUsage(
    val networkDiskUsed: Long,
    val imageMemoryUsed: Long,
    val imageMemoryMax: Long,
    val imageDiskUsed: Long,
)

data class PreferencesPageState(
    val runningJobs: List<StashJob> = emptyList(),
    val cacheUsage: CacheUsage =
        CacheUsage(
            networkDiskUsed = 0L,
            imageMemoryUsed = 0L,
            imageMemoryMax = 0L,
            imageDiskUsed = 0L,
        ),
)

suspend fun clearCaches(context: Context) =
    withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            Constants.getNetworkCache(context).evictAll()
        }
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
    }
