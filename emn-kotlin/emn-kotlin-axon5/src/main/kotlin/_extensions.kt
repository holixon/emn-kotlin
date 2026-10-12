package io.holixon.emn.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.*
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.generator.spi.SchemaDeclarationContext
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.SimpleName

/**
 * Get [SimpleName] from [FlowNode]'s typeReference name.
 */
@OptIn(ExperimentalKotlinPoetApi::class)
val FlowNode.simpleName: SimpleName get() = KotlinCodeGeneration.name.simpleName(this.typeReference.name)

fun Slice.isCommandSliceWithAvroTypeDefinitionRef(): Boolean {
  val sliceCommands = this.flowElements.commands() // contain exactly one command
  return sliceCommands.size == 1
    && sliceCommands.first().hasAvroTypeDefinitionRef()
    && this.flowElements.events()
    .containsAll(sliceCommands.first().possibleEvents()) // all events are in the slice
}

fun AggregateLane.hasAvroTypeDefinitionRef() = this.idSchema.getAvroTypeDefinitionRef() != null

fun FlowNode.hasAvroTypeDefinitionRef() = this.typeReference.hasAvroTypeDefinitionRef()

fun FlowNodeType.hasAvroTypeDefinitionRef() = this.schema.getAvroTypeDefinitionRef() != null

fun Schema?.getAvroTypeDefinitionRef(): EmbeddedSchema? {
  return this?.let {
    if (this.schemaFormat == "avro-type-reference" && this is EmbeddedSchema) {
      this
    } else {
      null
    }
  }
}

fun ElementValue?.getEmbeddedJsonValueAsMap(objectMapper: ObjectMapper): Map<String, Any>? {
  return this?.let {
    if (this.valueFormat == "application/json" && this is EmbeddedValue) {
      objectMapper.readValue(
        this.content,
        objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java)
      )
    } else {
      null
    }
  }
}

@Deprecated("moved to Specification")
val Specification.testMethodName: String
  get() = id

val SchemaDeclarationContext.emnContext: EmnGenerationContext get() = this.tag(EmnGenerationContext::class)!!
val ProtocolDeclarationContext.emnContext: EmnGenerationContext get() = this.tag(EmnGenerationContext::class)!!
