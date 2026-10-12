package io.holixon.emn.generation.specification

import com.squareup.kotlinpoet.CodeBlock
import io.holixon.emn.generation.getEmbeddedJsonValueAsMap
import io.holixon.emn.generation.instantiateMessageDirectly
import io.holixon.emn.generation.instantiateMessageWithInstancio
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.FlowNode

/** Compatibility adapter for the original AVRO-backed specification test generator. */
class AvroMessageValueRenderer(private val context: EmnGenerationContext) : MessageValueRenderer {

  override fun render(message: FlowNode): CodeBlock {
    val elementValue = requireNotNull(message.value) { "Element $message must have a value." }
    val properties = requireNotNull(elementValue.getEmbeddedJsonValueAsMap(context.objectMapper)) {
      "Could not parse value of $message as a map of properties."
    }
    val avroPoetType = context.avroTypes.messages.single { it.nodeType == message.typeReference }.poetType
    return when (context.properties.instanceCreator) {
      "instancio" -> instantiateMessageWithInstancio(
        avroPoetType,
        context.protocolDeclarationContext.avroPoetTypes,
        properties
      )
      else -> instantiateMessageDirectly(
        avroPoetType,
        context.protocolDeclarationContext.avroPoetTypes,
        properties
      )
    }
  }
}
