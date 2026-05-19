package com.github.damontecres.stashapp.di.server

import androidx.core.net.toUri
import com.github.damontecres.stashapp.util.isNotNullOrBlank

/**
 * Represents a server
 */
data class StashServer(
    val url: String,
    val apiKey: String?,
) {
    override fun toString(): String = "StashServer(url=$url, apiKey?=${apiKey.isNotNullOrBlank()})"

    val cleanedApiKey by lazy { apiKey?.trim() }
    val serverRoot by lazy { getServerRoot(url) }

    val serverKey get() = url.replace(Regex("[^\\w.]"), "_")

    companion object {
        private const val SERVER_PREF_PREFIX = "server_"
        private const val SERVER_APIKEY_PREF_PREFIX = "apikey_"

        val UNSET = StashServer("UNSET", null)

        /**
         * Get the server URL excluding the (unlikely) `/graphql` last path segment
         *
         * Basically, if a user is using a reverse proxy routes the path ending with `/graphql`, this will remove that
         */
        private fun getServerRoot(stashUrl: String): String {
            var cleanedStashUrl = stashUrl.trim()
            if (!cleanedStashUrl.startsWith("http://") && !cleanedStashUrl.startsWith("https://")) {
                // Assume http
                cleanedStashUrl = "http://$cleanedStashUrl"
            }
            var url = cleanedStashUrl.toUri()
            val pathSegments = url.pathSegments.toMutableList()
            if (pathSegments.isNotEmpty() && pathSegments.last() == "graphql") {
                pathSegments.removeAt(pathSegments.size - 1)
            }
            url =
                url
                    .buildUpon()
                    .path(pathSegments.joinToString("/"))
                    .build()
            return url.toString()
        }
    }
}
