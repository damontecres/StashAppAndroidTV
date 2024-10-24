package com.github.damontecres.stashapp.util

import android.content.Context
import com.apollographql.apollo.ApolloClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object Provider {
    @Provides
    @ViewModelScoped
    fun createApolloClient(server: StashServer): ApolloClient {
        return StashClient.getApolloClient(server)
    }

    @Provides
    @ViewModelScoped
    fun stashServer(
        @ApplicationContext context: Context,
    ): StashServer {
        return StashServer.getCurrentStashServer(context)!!
    }
}
