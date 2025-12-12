package com.github.damontecres.stashapp.ui.components.prefs

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.github.damontecres.stashapp.api.JobQueueQuery
import com.github.damontecres.stashapp.api.fragment.StashJob
import com.github.damontecres.stashapp.api.type.JobStatusUpdateType
import com.github.damontecres.stashapp.ui.indexOfFirstOrNull
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.SubscriptionEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PreferencesViewModel : ViewModel() {
    private val lock = Mutex()
    val runningJobs = MutableLiveData<List<StashJob>>(listOf())
    val cacheUsage =
        MutableLiveData<CacheUsage>(
            CacheUsage(
                networkDiskUsed = 0L,
                imageMemoryUsed = 0L,
                imageMemoryMax = 0L,
                imageDiskUsed = 0L,
            ),
        )

    fun init(
        context: Context,
        server: StashServer,
    ) {
        runningJobs.value = listOf()
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val queryEngine = QueryEngine(server)
            runningJobs.value =
                queryEngine
                    .executeQuery(JobQueueQuery())
                    .data
                    ?.jobQueue
                    ?.map { it.stashJob }
                    .orEmpty()

            val subscriptionEngine = SubscriptionEngine(server)
            subscriptionEngine.subscribeToJobs { update ->
                val type = update.jobsSubscribe.type
                val jobData = update.jobsSubscribe.job.stashJob
                viewModelScope.launch(StashCoroutineExceptionHandler()) {
                    handleUpdate(type, jobData)
                }
            }
        }
        updateCacheUsage(context)
    }

    private suspend fun handleUpdate(
        type: JobStatusUpdateType,
        jobData: StashJob,
    ) {
        lock.withLock {
            val mutable = runningJobs.value!!.toMutableList()
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
                            val mutable = runningJobs.value!!.toMutableList()
                            if (mutable.removeIf { it.id == jobData.id }) {
                                runningJobs.value = mutable
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
            runningJobs.value = mutable
        }
    }

    fun updateCacheUsage(context: Context) {
        val networkDisk = Constants.getNetworkCache(context).size()
        val imageUsedMemory = context.imageLoader.memoryCache?.size ?: 0L
        val imageMaxMemory = context.imageLoader.memoryCache?.maxSize ?: 0L
        val imageDisk = context.imageLoader.diskCache?.size ?: 0L
        cacheUsage.value =
            CacheUsage(
                networkDiskUsed = networkDisk,
                imageMemoryUsed = imageUsedMemory,
                imageMemoryMax = imageMaxMemory,
                imageDiskUsed = imageDisk,
            )
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
