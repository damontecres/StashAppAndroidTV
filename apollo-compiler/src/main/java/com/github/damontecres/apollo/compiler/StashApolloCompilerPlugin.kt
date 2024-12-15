package com.github.damontecres.apollo.compiler

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * An [ApolloCompilerPlugin] to add some extra annotations and interfaces to the classes generated from the graphql schema
 */
class StashApolloCompilerPlugin : ApolloCompilerPlugin {
    override fun kotlinOutputTransform(): Transform<KotlinOutput> {
        return object : Transform<KotlinOutput> {
            override fun transform(input: KotlinOutput): KotlinOutput {
                val packageName = "com.github.damontecres.stashapp"
                val stashDataInterface = ClassName("$packageName.data", "StashData")
                val stashFilterInterface = ClassName("$packageName.api.type", "StashDataFilter")
                val stashFilterFileSpec =
                    FileSpec
                        .builder(stashFilterInterface)
                        .addType(
                            TypeSpec
                                .interfaceBuilder(stashFilterInterface)
                                .addModifiers(KModifier.SEALED)
                                .addAnnotation(Serializable::class)
                                .build(),
                        ).build()

                val newFileSpecs =
                    input.fileSpecs.map { file ->
                        if (file.name.endsWith("FilterType") || file.name.endsWith("CriterionInput")) {
                            // Modify filter or filter input types
                            handleFilterInput(file, stashFilterInterface)
                        } else if (file.name.endsWith("Data")) {
                            // Modify data types
                            // Note that fragments for data types by convention are suffixed with "Data"
                            handleData(file, stashDataInterface)
                        } else {
                            file
                        }
                    }
                return KotlinOutput(
                    newFileSpecs + stashFilterFileSpec,
                    input.codegenMetadata,
                )
            }
        }
    }

    @OptIn(DelicateKotlinPoetApi::class)
    private fun handleFilterInput(
        file: FileSpec,
        stashFilterInterface: ClassName,
    ): FileSpec {
        val builder = file.toBuilder()
        builder.members.replaceAll { member ->
            if (member is TypeSpec) {
                // Mark as Serializable
                val typeBuilder =
                    member
                        .toBuilder()
                        .addAnnotation(Serializable::class.java)
                typeBuilder.propertySpecs.replaceAll { prop ->
                    if (prop.type is ParameterizedTypeName &&
                        (prop.type as ParameterizedTypeName).rawType.canonicalName == "com.apollographql.apollo.api.Optional"
                    ) {
                        // If the property is an Optional (basically all of them), then add a Contextual annotation
                        // This allows for runtime serialization, because the app defines a serializer for this class
                        prop
                            .toBuilder()
                            .addAnnotation(Contextual::class)
                            .build()
                    } else {
                        prop
                    }
                }

                // If the type is a filter, add the interface
                if (member.name!!.endsWith("FilterType")) {
                    typeBuilder.addSuperinterface(stashFilterInterface)
                }

                typeBuilder.build()
            } else {
                member
            }
        }
        return builder.build()
    }

    private fun handleData(
        file: FileSpec,
        stashDataInterface: ClassName,
    ): FileSpec {
        val builder = file.toBuilder()
        builder.members.replaceAll { member ->
            if (member is TypeSpec && member.propertySpecs.find { it.name == "id" } != null) {
                // Mark the type with the data interface
                val memberBuilder =
                    member
                        .toBuilder()
                        .addSuperinterface(stashDataInterface)
                memberBuilder.propertySpecs.replaceAll {
                    if (it.name == "id") {
                        // If the property is named id, need to add override due to the super interface
                        it
                            .toBuilder()
                            .addModifiers(KModifier.OVERRIDE)
                            .build()
                    } else {
                        it
                    }
                }
                memberBuilder.build()
            } else {
                member
            }
        }

        return builder.build()
    }
}
