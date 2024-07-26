package com.github.damontecres.stashapp.util

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.github.damontecres.stashapp.api.ConfigurationQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class QueryRepository
    @Inject
    constructor(
        private val apolloClient: ApolloClient,
    ) {
        enum class QueryResultStatus {
            SUCCESS,
            FAILURE,
        }

        data class QueryResult<T>(val status: QueryResultStatus, val data: T)

        private suspend fun <D : Operation.Data> executeQuery(query: ApolloCall<D>): ApolloResponse<D> =
            withContext(Dispatchers.IO) {
//            val queryName = query.operation.name()
//            val id = QUERY_ID.getAndIncrement()
//            Log.v(TAG, "executeQuery $id $queryName")
                query.execute()
            }

        suspend fun getServerConfiguration(): ConfigurationQuery.Data? {
            val query = ConfigurationQuery()
            val result = apolloClient.query(query).execute()
            return result.data
        }
    }
