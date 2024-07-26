package com.github.damontecres.stashapp.util

import com.apollographql.apollo3.ApolloClient
import com.github.damontecres.stashapp.StashApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object Provider {
    @Provides
    fun createApolloClient(): ApolloClient {
        return StashClient.getApolloClient(StashApplication.getApplication())
    }
}
