package com.github.damontecres.stashapp.ui.components.server

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.ApolloClient
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.CredentialsQuery
import com.github.damontecres.stashapp.api.GenerateApiKeyMutation
import com.github.damontecres.stashapp.di.StandardHttpClient
import com.github.damontecres.stashapp.di.server.MutationEngine
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.server.StashApi
import com.github.damontecres.stashapp.di.server.StashServer
import com.github.damontecres.stashapp.di.services.InterfaceService
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.SetupNavigationManager
import com.github.damontecres.stashapp.navigation.SetupDestination
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.TRUST_ALL_CERTS
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.launchDefault
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.KoinViewModel
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

@KoinViewModel
class ManageServersViewModel(
    private val context: Application,
    private val api: StashApi,
    @param:StandardHttpClient private val httpClient: OkHttpClient,
    private val serverRepository: ServerRepository,
    private val navigationManager: NavigationManager,
    private val setupNavigationManager: SetupNavigationManager,
    private val interfaceService: InterfaceService,
) : ViewModel() {
    val currentServer get() = serverRepository.currentServer

    private val _state = MutableStateFlow(ManageServersState())
    val state: StateFlow<ManageServersState> = _state

    init {
        viewModelScope.launchIO {
            interfaceService.setTitle(context.getString(R.string.manage_servers))
            val servers = serverRepository.getAll()
            _state.update {
                it.copy(
                    allServers = servers,
                    serverStatus = servers.associateWith { ServerTestResult.Pending },
                )
            }
            servers.forEach { server ->
                testServer(server)
            }
        }
    }

    fun clearConnectionStatus() {
        _state.update { it.copy(connectionState = ConnectionState.Inactive) }
    }

    fun testServer(server: StashServer) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    serverStatus =
                        it.serverStatus
                            .toMutableMap()
                            .apply { put(server, ServerTestResult.Pending) },
                )
            }
            val apolloClient = StashApi.createApolloClient(server, httpClient)
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
            _state.update {
                it.copy(
                    serverStatus = it.serverStatus.toMutableMap().apply { put(server, testResult) },
                )
            }
        }
    }

    fun removeServer(server: StashServer) {
        viewModelScope.launchIO {
            serverRepository.removeStashServer(server)
            val servers = serverRepository.getAll()
            _state.update {
                it.copy(
                    allServers = servers,
                    serverStatus = it.serverStatus.toMutableMap().apply { remove(server) },
                )
            }
        }
    }

    fun addServer(server: StashServer) {
        viewModelScope.launchIO {
            serverRepository.addServer(server)
            val servers = serverRepository.getAll()
            _state.update { it.copy(allServers = servers) }
        }
    }

    fun addServerAsCurrent(server: StashServer) {
        viewModelScope.launchIO {
            serverRepository.addServer(server)
            serverRepository.setCurrentStashServer(server)
            val servers = serverRepository.getAll()
            _state.update { it.copy(allServers = servers) }
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
                _state.update { it.copy(connectionState = ConnectionState.Inactive) }
                if (serverUrl.isNotNullOrBlank()) {
                    if (serverUrl in state.value.allServers.map { it.url }) {
                        _state.update { it.copy(connectionState = ConnectionState.DuplicateServer) }
                    } else {
                        _state.update { it.copy(connectionState = ConnectionState.Testing) }
                        delay(300L)
                        try {
                            if (useUsername && username.isNotNullOrBlank() && apiKey.isNotNullOrBlank()) {
                                testWithUsername(serverUrl, username, apiKey, trustCerts)
                            } else {
                                val server = StashServer(serverUrl, apiKey?.ifBlank { null })
                                val apolloClient = StashApi.createApolloClient(server, httpClient)
                                val result = testStashConnection(context, false, apolloClient)
                                if (result is TestResult.Error && result.exception is CancellationException) {
                                    _state.update { it.copy(connectionState = ConnectionState.Inactive) }
                                } else {
                                    _state.update {
                                        it.copy(
                                            connectionState =
                                                ConnectionState.Result(
                                                    result,
                                                ),
                                        )
                                    }
                                }
                            }
                        } catch (_: CancellationException) {
                            _state.update { it.copy(connectionState = ConnectionState.Inactive) }
                        } catch (ex: Exception) {
                            _state.update {
                                it.copy(
                                    connectionState =
                                        ConnectionState.Result(
                                            TestResult.Error(ex.localizedMessage, ex),
                                        ),
                                )
                            }
                        }
                    }
                }
                Log.d(TAG, "connectionState=${state.value.connectionState}")
            }
    }

    private suspend fun testWithUsername(
        serverUrl: String,
        username: String,
        password: String,
        trustCerts: Boolean,
    ) {
        try {
            val httpClient = createCookieHttpClient(trustCerts)
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
                _state.update {
                    it.copy(connectionState = ConnectionState.Result(TestResult.AuthRequired))
                }
            } else {
                val testApi = api.createFor(StashServer(serverUrl, null), httpClient)
                val queryEngine = QueryEngine(testApi)
                val mutationEngine = MutationEngine(testApi)

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
                        _state.update {
                            it.copy(
                                connectionState =
                                    ConnectionState.Result(
                                        TestResult.Error(
                                            "Failed to generate API Key",
                                            genResult.exception,
                                        ),
                                    ),
                            )
                        }
                    } else {
                        currentApiKey = newApiKey
                    }
                }
                _state.update { it.copy(connectionState = ConnectionState.NewApiKey(currentApiKey)) }
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Exception generating api key", ex)
            _state.update {
                it.copy(
                    connectionState =
                        ConnectionState.Result(TestResult.Error(ex.localizedMessage, ex)),
                )
            }
        }
    }

    /**
     * Build an [ApolloClient] suitable for testing connectivity for the specified server.
     */
    fun createCookieHttpClient(trustCerts: Boolean): OkHttpClient {
        var builder =
            httpClient
                .newBuilder()
                .cookieJar(
                    object : CookieJar {
                        private val cookies = mutableMapOf<String, List<Cookie>>()

                        override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies[url.host] ?: listOf()

                        override fun saveFromResponse(
                            url: HttpUrl,
                            cookies: List<Cookie>,
                        ) {
                            this.cookies[url.host] = cookies
                        }
                    },
                ).readTimeout(7, TimeUnit.SECONDS)
                .writeTimeout(7, TimeUnit.SECONDS)

        if (trustCerts) {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf(TRUST_ALL_CERTS), SecureRandom())
            builder =
                builder
                    .sslSocketFactory(
                        sslContext.socketFactory,
                        TRUST_ALL_CERTS,
                    ).hostnameVerifier { _, _ ->
                        true
                    }
        }
        return builder.build()
    }

    fun switchServer(server: StashServer) {
        viewModelScope.launchDefault {
            serverRepository.setCurrentStashServer(server)
            navigationManager.reloadMain()
            setupNavigationManager.navigateTo(SetupDestination.AppContent(server))
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

data class ManageServersState(
    val allServers: List<StashServer> = emptyList(),
    val serverStatus: Map<StashServer, ServerTestResult> = emptyMap(),
    val connectionState: ConnectionState = ConnectionState.Inactive,
)
