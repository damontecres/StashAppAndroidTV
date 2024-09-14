package com.github.damontecres.stashapp.util

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Subscription
import com.github.damontecres.stashapp.api.JobProgressSubscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class SubscriptionEngine(
    server: StashServer,
    client: ApolloClient = StashClient.getApolloClient(server),
) : StashEngine(server, client) {
    private suspend fun <D : Subscription.Data> executeSubscription(
        subscription: Subscription<D>,
        consumer: (D) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val name = subscription.name()
        val id = OPERATION_ID.getAndIncrement()

        client.subscription(subscription).toFlow().collect { response ->
            if (response.data != null) {
                Log.v(TAG, "executeSubscription $id $name response received")
                withContext(Dispatchers.Main) {
                    consumer.invoke(response.data!!)
                }
            } else if (response.exception != null) {
                throw createException(id, name, response.exception!!) { msg, ex ->
                    SubscriptionException(id, name, msg, ex)
                }
            } else {
                val errorMsgs = response.errors!!.joinToString("\n") { it.message }
                Log.e(TAG, "Errors in $id $name: ${response.errors}")
                throw SubscriptionException(id, name, "Error in $name: $errorMsgs")
            }
        }
    }

    suspend fun subscribeToJobs(consumer: (JobProgressSubscription.Data) -> Unit) {
        val subscription = JobProgressSubscription()
        executeSubscription(subscription, consumer)
    }

    companion object {
        private const val TAG = "SubscriptionEngine"
        private val OPERATION_ID = AtomicInteger(0)
    }

    open class SubscriptionException(
        id: Int,
        mutationName: String,
        msg: String? = null,
        cause: Exception? = null,
    ) : ServerCommunicationException(id, mutationName, msg, cause)
}
