package io.holixon.emn.generation

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
  val commandAvroType = requireNotNull(context.protocol.types.values.first {
    it.schema.canonicalName == CanonicalName.parse(schemaReference)
  }) { "Referenced unknown type $schemaReference" }
  return context.avroPoetTypes[commandAvroType.hashCode]
}

