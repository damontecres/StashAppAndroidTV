package com.github.damontecres.stashapp.util

import android.util.Log
import com.apollographql.apollo.api.Subscription
import com.github.damontecres.stashapp.api.JobProgressSubscription
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Engine for handling graphql subscriptions
 *
 * @param server the stash server to connect to
 * @param client the client to use, defaults to one for the server
 * @param ioDispatcher the dispatcher to use for general I/O operations
 * @param callbackDispatcher the dispatcher to use for running callbacks from subscription results
 */
class SubscriptionEngine(
    server: StashServer,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val callbackDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : StashEngine(server) {
    private suspend fun <D : Subscription.Data> executeSubscription(
        subscription: Subscription<D>,
        consumer: (D) -> Unit,
    ) = withContext(ioDispatcher) {
        val name = subscription.name()
        val id = OPERATION_ID.getAndIncrement()

        client.subscription(subscription).toFlow().collect { response ->
            if (response.data != null) {
                Log.v(TAG, "executeSubscription $id $name response received")
                withContext(callbackDispatcher) {
                    consumer.invoke(response.data!!)
                }
            } else if (response.exception != null) {
                throw createException(id, name, response.exception!!) { msg, ex ->
                    SubscriptionException(id, name, msg, ex)
                }
            } else {
                val errorMessages = response.errors!!.joinToString("\n") { it.message }
                Log.e(TAG, "Errors in $id $name: ${response.errors}")
                throw SubscriptionException(id, name, "Error in $name: $errorMessages")
            }
        }
        Log.v(TAG, "Completed subscription $id $name")
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
