package io.holixon.emn.generation

import io.holixon.emn.model.*
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.value.CanonicalName

fun Slice.isCommandSlice(): Boolean {
  val sliceCommands = this.flowElements.commands() // contain exactly one command
  return sliceCommands.size == 1
    && sliceCommands.first().hasAvroTypeDefinition()
    && this.flowElements.events()
    .containsAll(sliceCommands.first().possibleEvents()) // all events are in the slice
}

fun Schema?.embeddedAvroSchema(): EmbeddedSchema? {
  return if (this != null
          && this.schemaFormat == "avro-type-reference"
          && this is EmbeddedSchema
    ) {
    this
  } else {
    null
  }
}


fun FlowNode.hasAvroTypeDefinition() = this.typeReference.hasAvroTypeDefinition()

fun FlowNodeType.hasAvroTypeDefinition() = this.schema != null
  && this.schema!!.schemaFormat == "avro-type-reference"
  && this.schema is EmbeddedSchema


fun FlowNodeType.resolveAvroPoetType(context: ProtocolDeclarationContext): AvroPoetType {
  val schemaReference = this.schemaReference()
  val commandAvroType = requireNotNull(context.protocol.types.values.first {
    it.schema.canonicalName == CanonicalName.parse(schemaReference)
  }) { "Referenced unknown type $schemaReference" }
  return context.avroPoetTypes[commandAvroType.hashCode]
}
