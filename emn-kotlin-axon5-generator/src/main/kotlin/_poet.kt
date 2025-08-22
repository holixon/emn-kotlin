@file:OptIn(ExperimentalKotlinPoetApi::class)

package io.holixon.emn.generation

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import org.axonframework.modelling.annotation.InjectEntity

fun injectEntityAnnotation(idProperty: String? = null) = buildAnnotation(InjectEntity::class) {
  // we need an id property for creation command handler
  idProperty?.let { addStringMember("idProperty", it) }
}

fun AvroPoetType.idProperty(): String? {
  // FIXME -> find a way how to model this.
  return null
}
