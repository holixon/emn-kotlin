package io.holixon.emn.generation

import io.holixon.emn.model.FlowElement
import io.holixon.emn.model.FlowElement.FlowNode.*
import io.holixon.emn.model.FlowElementType
import io.holixon.emn.model.Schema
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.value.CanonicalName

fun String.removeSpaces() = this.replace(" ", "")



fun List<FlowElement>.commands() = filterIsInstance<Command>()
fun List<FlowElement>.events() = filterIsInstance<Event>()
fun List<FlowElement>.views() = filterIsInstance<View>()
fun List<FlowElement>.queries() = filterIsInstance<Query>()

fun Command.possibleEvents() = this.outgoing.map { flow -> flow.target }.events()

fun Command.views() = this.incoming.map { flow -> flow.source }.views()

fun View.queries() = this.incoming.map { flow -> flow.source }.queries()

fun Query.events() = this.incoming.map { flow -> flow.source }.events()

fun Event.commands() = this.incoming.map { flow -> flow.source }.commands()

fun FlowElement.FlowNode.hasAvroTypeDefinition() = this.typeReference.schema != null
  && this.typeReference.schema!!.schemaFormat == "avro-type-reference"
  && this.typeReference.schema is Schema.EmbeddedSchema

fun FlowElementType.FlowNodeType.schemaReference(): String {
  requireNotNull(this.schema) { "No schema found for $this" }
  return (this.schema!! as Schema.EmbeddedSchema).content
}

fun FlowElementType.FlowNodeType.resolveAvroPoetType(context: ProtocolDeclarationContext): AvroPoetType {
  val schemaReference = this.schemaReference()
  val commandAvroType = requireNotNull(context.protocol.types.values.first {
    it.schema.canonicalName == CanonicalName.parse(schemaReference)
  }) { "Referenced unknown type $schemaReference" }
  return context.avroPoetTypes[commandAvroType.hashCode]
}
