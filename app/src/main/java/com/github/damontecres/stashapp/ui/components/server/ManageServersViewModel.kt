package com.github.damontecres.stashapp.ui.components.server

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ManageServersViewModel : ViewModel() {
    val allServers = MutableLiveData<List<StashServer>>(listOf())
    val serverStatus = MutableLiveData<Map<StashServer, ServerTestResult>>(mapOf())

    val connectionState = MutableLiveData<ConnectionState>(ConnectionState.Inactive)

    init {
        val servers = StashServer.getAll(StashApplication.getApplication())
        allServers.value = servers
        serverStatus.value = servers.associateWith { ServerTestResult.Pending }
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            servers.forEach { server ->
                testServer(server)
            }
        }
    }

    private suspend fun testServer(server: StashServer): ServerTestResult {
        val result =
            testStashConnection(
                StashApplication.getApplication(),
                false,
                server.apolloClient,
            )
        val testResult =
            when (result) {
                TestResult.AuthRequired,
                is TestResult.Error,
                TestResult.SelfSignedCertRequired,
                TestResult.SslRequired,
                is TestResult.UnsupportedVersion,
                -> ServerTestResult.Error(result)

                is TestResult.Success -> ServerTestResult.Success
            }
        serverStatus.value = serverStatus.value!!.toMutableMap().apply { put(server, testResult) }
        return testResult
    }

    fun removeServer(server: StashServer) {
        StashServer.removeStashServer(StashApplication.getApplication(), server)
        val servers = StashServer.getAll(StashApplication.getApplication())
        allServers.value = servers
        serverStatus.value = serverStatus.value!!.toMutableMap().apply { remove(server) }
    }

    fun addServer(server: StashServer) {
        StashServer.addServer(StashApplication.getApplication(), server)
        val servers = StashServer.getAll(StashApplication.getApplication())
        allServers.value = servers
    }

    private var testServerJob: Job? = null

    fun testServer(
        serverUrl: String,
        apiKey: String?,
        trustCerts: Boolean,
    ) {
        testServerJob?.cancel()
        testServerJob =
            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                val context = StashApplication.getApplication()
                connectionState.value = ConnectionState.Inactive
                if (serverUrl.isNotNullOrBlank()) {
                    if (serverUrl in allServers.value!!.map { it.url }) {
                        connectionState.value = ConnectionState.DuplicateServer
                    } else {
                        connectionState.value = ConnectionState.Testing
                        delay(500L)
                        try {
                            val apolloClient =
                                StashClient.createTestApolloClient(
                                    context,
                                    StashServer(serverUrl, apiKey?.ifBlank { null }),
                                    trustCerts,
                                )
                            val result = testStashConnection(context, false, apolloClient)
                            connectionState.value = ConnectionState.Result(result)
                        } catch (_: CancellationException) {
                            connectionState.value = ConnectionState.Inactive
                        } catch (ex: Exception) {
                            connectionState.value =
                                ConnectionState.Result(TestResult.Error(ex.localizedMessage, ex))
                        }
                    }
                }
                Log.d(TAG, "connectionState=$connectionState")
            }
    }

    companion object {
        private const val TAG = "ManageServersViewModel"
    }
}

sealed interface ServerTestResult {
    data object Pending : ServerTestResult

    data object Success : ServerTestResult

    data class Error(
        val result: TestResult,
    ) : ServerTestResult
}
