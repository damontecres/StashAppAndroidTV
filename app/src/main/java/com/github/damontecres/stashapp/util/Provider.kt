package com.github.damontecres.stashapp.util

import android.content.Context
import com.apollographql.apollo3.ApolloClient
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
    fun createApolloClient(
        @ApplicationContext context: Context,
    ): ApolloClient {
        return StashClient.getApolloClient(context)
    }
}
