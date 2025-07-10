package com.github.damontecres.stashapp.ui.components.server

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResultStatus
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
            // TODO parallelize?
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
        // TODO better error messages
        val testResult =
            when (result.status) {
                TestResultStatus.SUCCESS -> ServerTestResult.Success
                TestResultStatus.AUTH_REQUIRED -> ServerTestResult.Error("Auth")
                TestResultStatus.ERROR -> ServerTestResult.Error("Error")
                TestResultStatus.UNSUPPORTED_VERSION -> ServerTestResult.Error("Version")
                TestResultStatus.SSL_REQUIRED -> ServerTestResult.Error("SSL")
                TestResultStatus.SELF_SIGNED_REQUIRED -> ServerTestResult.Error("Cert")
            }
        serverStatus.value = serverStatus.value!!.toMutableMap().apply { put(server, testResult) }
        return testResult
    }

    fun removeServer(server: StashServer) {
        StashServer.removeStashServer(StashApplication.getApplication(), server)
        val servers = StashServer.getAll(StashApplication.getApplication())
        allServers.value = servers
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
        val message: String,
    ) : ServerTestResult
}
