package io.holixon.emn.generation

import io.github.oshai.kotlinlogging.KLogger
import io.holixon.emn.model.*
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.value.CanonicalName

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

