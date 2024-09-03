package com.github.damontecres.apollo.compiler

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

class StashApolloCompilerPlugin : ApolloCompilerPlugin {
    override fun kotlinOutputTransform(): Transform<KotlinOutput> {
        return object : Transform<KotlinOutput> {
            override fun transform(input: KotlinOutput): KotlinOutput {
                val sampleFileSpec = input.fileSpecs.first()
                val packageName = "com.github.damontecres.stashapp.api"
                val stashDataInterface = ClassName("$packageName.fragment", "StashData")
                val stashDataFileSpec =
                    FileSpec.builder(stashDataInterface)
                        .addType(
                            TypeSpec.interfaceBuilder(stashDataInterface)
                                .addModifiers(KModifier.SEALED)
                                .addProperty("id", String::class)
                                .build(),
                        )
                        .build()

                val stashFilterInterface = ClassName("$packageName.type", "StashDataFilter")
                val stashFilterFileSpec =
                    FileSpec.builder(stashFilterInterface)
                        .addType(
                            TypeSpec.interfaceBuilder(stashFilterInterface)
                                .addModifiers(KModifier.SEALED)
                                .addAnnotation(Serializable::class)
                                .build(),
                        )
                        .build()

                val newFileSpecs =
                    input.fileSpecs.map { file ->
                        if (file.name.endsWith("FilterType") || file.name.endsWith("CriterionInput")) {
                            handleFilterInput(file, stashFilterInterface)
                        } else if (file.name.endsWith("Data")) {
                            handleData(file, stashDataInterface)
                        } else {
                            file
                        }
                    }
                return KotlinOutput(
                    newFileSpecs + stashDataFileSpec + stashFilterFileSpec,
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
                val typeBuilder =
                    member.toBuilder()
                        .addAnnotation(Serializable::class.java)
                typeBuilder.propertySpecs.replaceAll { prop ->
                    if (prop.type is ParameterizedTypeName &&
                        (prop.type as ParameterizedTypeName).rawType.canonicalName == "com.apollographql.apollo.api.Optional"
                    ) {
                        prop.toBuilder()
                            .addAnnotation(Contextual::class)
                            .build()
                    } else {
                        prop
                    }
                }

                if (member.name!!.endsWith("FilterType")) {
                    typeBuilder.addSuperinterface(stashFilterInterface)
                }

                typeBuilder.build()
            } else if (member is PropertySpec) {
            } else {
                member
            }
        }
        return builder.build()
    }

    @OptIn(DelicateKotlinPoetApi::class)
    private fun handleData(
        file: FileSpec,
        stashDataInterface: ClassName,
    ): FileSpec {
        val builder = file.toBuilder()
        builder.members.replaceAll { member ->
            if (member is TypeSpec && member.propertySpecs.find { it.name == "id" } != null) {
                val memberBuilder =
                    member.toBuilder()
                        .addSuperinterface(stashDataInterface)
                // TODO: adding Serializable i
//                        .addAnnotation(Serializable::class)
                memberBuilder.propertySpecs.replaceAll {
                    if (it.name == "id") {
                        it.toBuilder()
                            .addModifiers(KModifier.OVERRIDE)
                            .build()
                    } else if (it.name == "updated_at" || it.name == "created_at") {
                        it.toBuilder()
//                            .addAnnotation(Contextual::class)
                            .build()
                    } else {
                        it
                    }
                }
                memberBuilder.typeSpecs.replaceAll { innerType ->
                    if (innerType.modifiers.contains(KModifier.DATA)) {
                        // Inner data class
                        innerType.toBuilder()
//                            .addAnnotation(Serializable::class)
                            .build()
                    } else {
                        innerType
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
