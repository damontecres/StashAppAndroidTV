package com.github.damontecres.stashapp.ui.components.server

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.CredentialsQuery
import com.github.damontecres.stashapp.api.GenerateApiKeyMutation
import com.github.damontecres.stashapp.di.server.MutationEngine
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.server.StashApi
import com.github.damontecres.stashapp.di.server.StashServer
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.testStashConnection
import com.github.damontecres.stashapp.views.models.EqualityMutableLiveData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ManageServersViewModel(
    private val context: Application,
    private val api: StashApi,
    private val serverRepository: ServerRepository,
) : ViewModel() {
    val currentServer get() = serverRepository.currentServer
    val allServers = MutableLiveData<List<StashServer>>(listOf())
    val serverStatus = MutableLiveData<Map<StashServer, ServerTestResult>>(mapOf())

    val connectionState = EqualityMutableLiveData<ConnectionState>(ConnectionState.Inactive)

    init {
        viewModelScope.launchIO {
            val servers = serverRepository.getAll()
            withContext(Dispatchers.Main) {
                allServers.value = servers
                serverStatus.value = servers.associateWith { ServerTestResult.Pending }
            }
            servers.forEach { server ->
                testServer(server)
            }
        }
    }

    fun clearConnectionStatus() {
        connectionState.value = ConnectionState.Inactive
    }

    fun testServer(server: StashServer) {
        viewModelScope.launch {
            serverStatus.value =
                serverStatus.value!!.toMutableMap().apply { put(server, ServerTestResult.Pending) }
            val apolloClient = api.createApolloClient(server)
            val result =
                testStashConnection(
                    context,
                    false,
                    apolloClient,
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
        viewModelScope.launchIO {
            serverRepository.removeStashServer(server)
            val servers = serverRepository.getAll()
            withContext(Dispatchers.Main) {
                allServers.value = servers
                serverStatus.value = serverStatus.value!!.toMutableMap().apply { remove(server) }
            }
        }
    }

    fun addServer(server: StashServer) {
        viewModelScope.launchIO {
            serverRepository.addServer(server)
            val servers = serverRepository.getAll()
            withContext(Dispatchers.Main) {
                allServers.value = servers
            }
        }
    }

    fun addServerAsCurrent(server: StashServer) {
        viewModelScope.launchIO {
            serverRepository.addServer(server)
            serverRepository.setCurrentStashServer(server)
            val servers = serverRepository.getAll()
            withContext(Dispatchers.Main) {
                allServers.value = servers
            }
        }
    }

    private var testServerJob: Job? = null

    fun testServer(
        serverUrl: String,
        apiKey: String?,
        trustCerts: Boolean,
        username: String?,
        useUsername: Boolean,
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
                        delay(300L)
                        try {
                            if (useUsername && username.isNotNullOrBlank() && apiKey.isNotNullOrBlank()) {
                                testWithUsername(serverUrl, username, apiKey, trustCerts)
                            } else {
                                val apolloClient =
                                    api.createApolloClient(
                                        StashServer(serverUrl, apiKey?.ifBlank { null }),
                                    )
                                val result = testStashConnection(context, false, apolloClient)
                                if (result is TestResult.Error && result.exception is CancellationException) {
                                    connectionState.value = ConnectionState.Inactive
                                } else {
                                    connectionState.value = ConnectionState.Result(result)
                                }
                            }
                        } catch (_: CancellationException) {
                            connectionState.value = ConnectionState.Inactive
                        } catch (ex: Exception) {
                            connectionState.value =
                                ConnectionState.Result(
                                    TestResult.Error(
                                        ex.localizedMessage,
                                        ex,
                                    ),
                                )
                        }
                    }
                }
                Log.d(TAG, "connectionState=${connectionState.value}")
            }
    }

    private suspend fun testWithUsername(
        serverUrl: String,
        username: String,
        password: String,
        trustCerts: Boolean,
    ) {
        try {
            TODO()
            val httpClient = StashClient.createCookieHttpClient(trustCerts)
            val loginUrl = StashClient.createLoginUrl(serverUrl)
            val request =
                Request
                    .Builder()
                    .url(loginUrl)
                    .post(
                        FormBody
                            .Builder()
                            .add("username", username)
                            .add("password", password)
                            .build(),
                    ).build()
            val response =
                withContext(Dispatchers.IO) {
                    httpClient.newCall(request).execute()
                }
            if (!response.isSuccessful) {
                connectionState.value = ConnectionState.Result(TestResult.AuthRequired)
            } else {
                val testApi = api.createFor(StashServer(serverUrl, null))
                val queryEngine = QueryEngine(testApi)
                val mutationEngine = MutationEngine(testApi, serverRepository)

                val res = queryEngine.executeQuery(CredentialsQuery())
                var currentApiKey =
                    res.data
                        ?.configuration
                        ?.general
                        ?.apiKey ?: ""
                if (currentApiKey.isBlank()) {
                    val genResult = mutationEngine.executeMutation(GenerateApiKeyMutation())
                    val newApiKey = genResult.data?.generateAPIKey
                    if (newApiKey.isNullOrBlank()) {
                        Log.w(
                            TAG,
                            "Exception generating api key: ${genResult.errors?.joinToString(",")}",
                            genResult.exception,
                        )
                        connectionState.value =
                            ConnectionState.Result(
                                TestResult.Error(
                                    "Failed to generate API Key",
                                    genResult.exception,
                                ),
                            )
                    } else {
                        currentApiKey = newApiKey
                    }
                }
                connectionState.value = ConnectionState.NewApiKey(currentApiKey)
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Exception generating api key", ex)
            connectionState.value =
                ConnectionState.Result(
                    TestResult.Error(
                        ex.localizedMessage,
                        ex,
                    ),
                )
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
