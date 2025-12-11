package com.github.damontecres.apollo.compiler

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
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
    @OptIn(ApolloExperimental::class)
    override fun beforeCompilationStep(
        environment: ApolloCompilerPluginEnvironment,
        registry: ApolloCompilerRegistry,
    ) {
        val packageName = "com.github.damontecres.stashapp"
        registry.registerKotlinOutputTransform(
            packageName,
            transform =
                object : Transform<KotlinOutput> {
                    override fun transform(input: KotlinOutput): KotlinOutput {
                        val stashDataInterface = ClassName("$packageName.api.fragment", "StashData")
                        val stashFilterInterface =
                            ClassName("$packageName.api.type", "StashDataFilter")
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
                                if (
                                    (
                                        file.name.endsWith("FilterType") &&
                                            file.name !in
                                            setOf(
                                                "FindFilterType",
                                                "SavedFindFilterType",
                                            )
                                    ) ||
                                    file.name.endsWith("CriterionInput")
                                ) {
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
                },
        )
    }

    private fun handleFilterInput(
        file: FileSpec,
        stashFilterInterface: ClassName,
    ): FileSpec {
        val builder = file.toBuilder()
        builder.members.replaceAll { member ->
            if (member is TypeSpec) {
                // Mark as Serializable
                val annotation =
                    if (member.name == "CustomFieldCriterionInput") {
                        AnnotationSpec
                            .builder(Serializable::class)
                            .addMember("with = com.github.damontecres.stashapp.util.CustomFieldCriterionInputSerializer::class")
                            .build()
                    } else {
                        AnnotationSpec.builder(Serializable::class).build()
                    }
                val typeBuilder =
                    member
                        .toBuilder()
                        .addAnnotation(annotation)
                if (member.name != "CustomFieldCriterionInput") {
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
