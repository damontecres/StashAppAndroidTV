package com.github.damontecres.apollo.compiler

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.Serializable

class StashApolloCompilerPlugin : ApolloCompilerPlugin {
    @OptIn(DelicateKotlinPoetApi::class)
    override fun kotlinOutputTransform(): Transform<KotlinOutput> {
        return object : Transform<KotlinOutput> {
            override fun transform(input: KotlinOutput): KotlinOutput {
                val sampleFileSpec = input.fileSpecs.first()
                val packageName = "com.github.damontecres.stashapp.api"
                val stashDataInterface = ClassName(packageName, "StashData")
                val stashDataFileSpec =
                    FileSpec.builder(stashDataInterface)
                        .addType(
                            TypeSpec.interfaceBuilder(stashDataInterface)
                                .addProperty("id", String::class)
                                .build(),
                        )
                        .build()

                val newFileSpecs =
                    input.fileSpecs.map { file ->
                        if (file.name.endsWith("FilterType") || file.name.endsWith("CriterionInput")) {
                            val builder = file.toBuilder()
                            builder.members.replaceAll { member ->
                                if (member is TypeSpec) {
                                    member.toBuilder()
                                        .addAnnotation(Serializable::class.java)
                                        .build()
                                } else {
                                    member
                                }
                            }

                            builder.build()
                        } else if (file.name.endsWith("Data")) {
                            val builder = file.toBuilder()
                            builder.members.replaceAll { member ->
                                if (member is TypeSpec && member.propertySpecs.find { it.name == "id" } != null) {
                                    val idBuilder =
                                        member.toBuilder()
                                            .addSuperinterface(stashDataInterface)
                                            .addAnnotation(Serializable::class.java)
                                    idBuilder.propertySpecs.replaceAll {
                                        if (it.name == "id") {
                                            it.toBuilder()
                                                .addModifiers(KModifier.OVERRIDE)
                                                .build()
                                        } else {
                                            it
                                        }
                                    }
                                    idBuilder.build()
                                } else {
                                    member
                                }
                            }

                            builder.build()
                        } else {
                            file
                        }
                    }
                return KotlinOutput(newFileSpecs + stashDataFileSpec, input.codegenMetadata)
            }
        }
    }
}
