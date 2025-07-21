package com.github.damontecres.stashapp.ui.components.server

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.launch

class ManageServersViewModel : ViewModel() {
    val allServers = MutableLiveData<List<StashServer>>(listOf())
    val serverStatus = MutableLiveData<Map<StashServer, ServerTestResult>>(mapOf())

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

    fun testServer(server: StashServer) {
        viewModelScope.launch {
            serverStatus.value =
                serverStatus.value!!.toMutableMap().apply { put(server, ServerTestResult.Pending) }
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
            serverStatus.value =
                serverStatus.value!!.toMutableMap().apply { put(server, testResult) }
        }
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
}

sealed interface ServerTestResult {
    data object Pending : ServerTestResult

    data object Success : ServerTestResult

    data class Error(
        val result: TestResult,
    ) : ServerTestResult
}
