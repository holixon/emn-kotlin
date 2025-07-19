package io.holixon.emn.generation.processor

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.avro.SchemaDeclarationContextExt.emnContext
import io.holixon.emn.generation.spi.commandHandlerClassName
import io.holixon.emn.model.FlowElementType.FlowNodeType.CommandType
import io.holixon.emn.model.FlowElementType.FlowNodeType.EventType
import io.toolisticon.kotlin.avro.generator.processor.KotlinDataClassFromRecordTypeProcessorBase
import io.toolisticon.kotlin.avro.generator.spi.SchemaDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordType
import io.toolisticon.kotlin.generation.builder.KotlinDataClassSpecBuilder
import io.toolisticon.kotlin.generation.poet.KDoc

@OptIn(ExperimentalKotlinPoetApi::class)
class MessageDataClassProcessor : KotlinDataClassFromRecordTypeProcessorBase() {

  override fun invoke(context: SchemaDeclarationContext, input: RecordType, builder: KotlinDataClassSpecBuilder): KotlinDataClassSpecBuilder {
    val emnContext = context.emnContext
    if (emnContext.isCommandType(input)) {
      val commandType = emnContext.avroReferenceTypes[input.canonicalName]!! as CommandType
      builder.addKdoc(KDoc.of("Command: ${input.name.value}"))

      val commandSlice = emnContext.commandSlices.firstOrNull { it.command.typeReference == commandType }
      if (commandSlice != null) {
        builder.addKdoc(KDoc.of("\nSlice: ${commandSlice.name}"))

        builder.addKdoc(KDoc.of("\n@see %T", commandSlice.commandHandlerClassName))
      }
    } else {
      val eventType = emnContext.avroReferenceTypes[input.canonicalName]!! as EventType

      // TODO : Empty in StudentSubscribedToCourse
//      val conceptNames = emnContext.events.filter { it.typeReference == eventType }
//        .map { emnContext.aggregates(it) }
//        .flatten()
//        .distinct()
//        .mapNotNull { it.name }
//        .joinToString()
      builder.addKdoc("Event: ${input.name.value}\n")
//      builder.addKdoc("Used in Concepts: ${conceptNames}\n")
    }

    return builder
  }

  override fun test(context: SchemaDeclarationContext, input: Any): Boolean {
    val emnContext = context.emnContext

    return super.test(context, input) && input is RecordType
      && (emnContext.isCommandType(input) || emnContext.isEventType(input))
  }


}
