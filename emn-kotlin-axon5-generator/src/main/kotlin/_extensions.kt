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

fun FlowElement.FlowNode.hasAvroTypeDefinition() = this.typeReference.schema != null
        && this.typeReference.schema!!.schemaFormat == "avro-type-reference"
        && this.typeReference.schema is Schema.EmbeddedSchema

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
