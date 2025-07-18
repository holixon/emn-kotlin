package io.holixon.emn.generation

import io.holixon.emn.model.*
import io.holixon.emn.model.FlowElement.FlowNode.Command
import io.holixon.emn.model.FlowElement.FlowNode.Event
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

fun Command.sourcedEvents(): List<Event> {
    val directEvents = this.views()
        .flatMap { view -> view.queries() }
        .map { query -> query.events() }
        .flatten()
    return directEvents + directEvents
        .filterNot { directEvents.contains(it) }
        .map { event ->
            event.commands().filterNot { it == this }
                .map { it.sourcedEvents() }.flatten()
        }.flatten()
}

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

fun AvroPoetType.idProperty(): String? {
    // FIXME -> find a way how to model this.
    return null
}
