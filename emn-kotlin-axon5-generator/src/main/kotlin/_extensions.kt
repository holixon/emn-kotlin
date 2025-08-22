package io.holixon.emn.generation

import io.holixon.emn.model.*
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.value.CanonicalName

fun String.removeSpaces() = this.replace(" ", "")


fun Slice.isCommandSlice(): Boolean {
  val sliceCommands = this.flowElements.commands() // contain exactly one command
  return sliceCommands.size == 1
    && sliceCommands.first().hasAvroTypeDefinition()
    && this.flowElements.events()
    .containsAll(sliceCommands.first().possibleEvents()) // all events are in the slice
}


fun FlowNode.hasAvroTypeDefinition() = this.typeReference.hasAvroTypeDefinition()

fun FlowNodeType.hasAvroTypeDefinition() = this.schema != null
  && this.schema!!.schemaFormat == "avro-type-reference"
  && this.schema is EmbeddedSchema


fun FlowNodeType.schemaReference(): String {
  requireNotNull(this.schema) { "No schema found for $this" }
  return (this.schema!! as EmbeddedSchema).content
}

fun FlowNodeType.resolveAvroPoetType(context: ProtocolDeclarationContext): AvroPoetType {
  val schemaReference = this.schemaReference()
  val commandAvroType = requireNotNull(context.protocol.types.values.first {
    it.schema.canonicalName == CanonicalName.parse(schemaReference)
  }) { "Referenced unknown type $schemaReference" }
  return context.avroPoetTypes[commandAvroType.hashCode]
}
