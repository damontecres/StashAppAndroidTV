package com.github.damontecres.apollo.compiler

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider

@OptIn(ApolloExperimental::class)
class StashApolloCompilerPluginProvider : ApolloCompilerPluginProvider {
    override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin = StashApolloCompilerPlugin()
}
