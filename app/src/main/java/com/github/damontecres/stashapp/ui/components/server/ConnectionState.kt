package com.github.damontecres.stashapp.ui.components.server

import com.github.damontecres.stashapp.util.TestResult

sealed interface ConnectionState {
    /**
     * Not doing anything, eg no testing and not result
     */
    data object Inactive : ConnectionState

    /**
     * Checking whether it can connect to a server
     */
    data object Testing : ConnectionState

    /**
     * Server url is a duplicate
     */
    data object DuplicateServer : ConnectionState

    /**
     * Result of the connection test
     */
    data class Result(
        val testResult: TestResult,
    ) : ConnectionState {
        val canConnect: Boolean = testResult is TestResult.Success || testResult is TestResult.AuthRequired
    }
}
