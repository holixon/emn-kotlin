package io.holixon.emn.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.github.oshai.kotlinlogging.KLogger
import io.holixon.emn.model.*
import io.toolisticon.kotlin.avro.generator.poet.AvroPoetType
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.value.CanonicalName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration

fun Slice.isCommandSliceWithAvroTypeDefinitionRef(): Boolean {
  val sliceCommands = this.flowElements.commands() // contain exactly one command
  return sliceCommands.size == 1
    && sliceCommands.first().hasAvroTypeDefinitionRef()
    && this.flowElements.events()
    .containsAll(sliceCommands.first().possibleEvents()) // all events are in the slice
}


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

fun FlowNodeType.resolveAvroPoetType(context: ProtocolDeclarationContext): AvroPoetType {
  val schemaReference = this.schemaReference()
  val commandAvroType = requireNotNull(context.protocol.types.values.firstOrNull {
    it.schema.canonicalName == CanonicalName.parse(schemaReference)
  }) { "Referenced unknown type $schemaReference" }
  return context.avroPoetTypes[commandAvroType.hashCode]
}

fun KLogger.noAggregateFoundLogger(emnElementType: FlowElementType) = {
  this.info { "No aggregate found for ${emnElementType.name}" }
}

fun KLogger.conflictingAggregatesFound(emnElementType: FlowElementType) = {
  this.warn { "Found conflicting EMN declaration, elements of type ${emnElementType.name} belong to different aggregate lanes." }
}


@OptIn(ExperimentalKotlinPoetApi::class)
val Specification.testMethodName: String get() {
  // Extract events from the "given" stage
  val eventNames = this.givenStage?.values?.events()?.map { event -> KotlinCodeGeneration.name.simpleName(event.typeReference.name) } ?: emptyList()

  // Format the "given" part
  val givenPart = if (eventNames.isEmpty()) {
    "givenNoEvents"
  } else {
    "given${eventNames.joinToString("And")}"
  }

  require(this.whenStage?.values?.commands() != null && this.whenStage?.values?.commands()!!.size == 1) {
    "Current implementation requires exactly one command in 'when' stage. Commands were ${this.whenStage?.values?.commands()}"
  }
  // Extract command from the "when" stage
  val commandName = this.whenStage?.values?.commands()?.firstOrNull()?.let {
    KotlinCodeGeneration.name.simpleName(it.typeReference.name)
  } ?: "NoCommand"
  val whenPart = "when$commandName"

  // Extract events and errors from the "then" stage
  val thenEventNames = this.thenStage?.values?.events()?.map { event -> KotlinCodeGeneration.name.simpleName(event.typeReference.name) } ?: emptyList()
  val thenErrorNames = this.thenStage?.values?.filterIsInstance<Error>()?.map { error -> KotlinCodeGeneration.name.simpleName(error.typeReference.name) } ?: emptyList()

  val thenNames = thenEventNames + thenErrorNames

  val thenPart = if (thenNames.isEmpty()) {
    "thenNoEvents"
  } else {
    "then${thenNames.joinToString("And")}"
  }

  // Combine all parts into the final method name
  return "${givenPart}_${whenPart}_$thenPart"
}
