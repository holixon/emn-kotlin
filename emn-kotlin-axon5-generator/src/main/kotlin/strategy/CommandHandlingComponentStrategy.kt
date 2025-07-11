package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.KModifier
import io.holixon.emn.generation.idProperty
import io.holixon.emn.generation.injectEntityAnnotation
import io.holixon.emn.generation.resolveAvroPoetType
import io.holixon.emn.generation.sourcedEvents
import io.holixon.emn.generation.spi.CommandSlice
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFun
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildInterface
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildParameter
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.classBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spec.KotlinFunSpec
import io.toolisticon.kotlin.generation.spec.KotlinInterfaceSpec
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy
import org.axonframework.commandhandling.annotation.CommandHandler
import org.axonframework.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.modelling.annotation.InjectEntity

@OptIn(ExperimentalKotlinPoetApi::class)
class CommandHandlingComponentStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, CommandSlice>(
  contextType = EmnGenerationContext::class, inputType = CommandSlice::class
) {
  override fun invoke(context: EmnGenerationContext, input: CommandSlice): KotlinFileSpecList {

    val slicePackage = input.packageName(context.rootPackageName + ".write")
    val handlerName = input.simpleClassName() + "CommandHandler"

    val fileBuilder = fileBuilder(ClassName(slicePackage, handlerName))

    val commandHandlerTypeBuilder = classBuilder(slicePackage, handlerName).apply {

      val commandType = input.command.typeReference.resolveAvroPoetType(context.protocolDeclarationContext)
      val sourcingEvents = input.command.sourcedEvents()
      val sourcingEventTypes = sourcingEvents.map { it.typeReference.resolveAvroPoetType(context.protocolDeclarationContext) }

      val idProperty = if (sourcingEvents.isEmpty()) { // no source events
        input.command.typeReference.resolveAvroPoetType(context.protocolDeclarationContext).idProperty()
      } else {
        null
      }

      val state = buildState(
        slicePackage = slicePackage,
        handlerName = handlerName,
        sourcingEventTypes = sourcingEventTypes
      )

      addFunction(
        buildHandler(
          commandType = commandType,
          stateSpec = state,
          idProperty = idProperty
        )
      )

      addType(state)
    }

    fileBuilder.addType(commandHandlerTypeBuilder.build())

    return KotlinFileSpecList(fileBuilder.build())
  }

  private fun buildHandler(commandType: AvroPoetType, stateSpec: KotlinInterfaceSpec, idProperty: String?): KotlinFunSpec {
    return buildFun("handle") {
      addAnnotation(
        buildAnnotation(CommandHandler::class) {
        }
      )
      this.addParameter("command", commandType.typeName)

      this.addParameter(
        buildParameter("state", stateSpec.className) {
          addAnnotation(injectEntityAnnotation(idProperty))
        }
      )

      this.addParameter("eventAppender", EventAppender::class)
    }
  }

  private fun buildState(slicePackage: String, handlerName: String, sourcingEventTypes: List<AvroPoetType>): KotlinInterfaceSpec {
    val className = ClassName(slicePackage, handlerName, "State")
    return buildInterface(className) {
      addAnnotation(
        buildAnnotation(EventSourcedEntity::class) {
          // FIXME: Add tag
        }
      )
      sourcingEventTypes.forEach { event ->
        addFunction(buildEventSourcingHandler(event, className))
      }
    }
  }

  private fun buildEventSourcingHandler(eventType: AvroPoetType, stateClassName: ClassName): KotlinFunSpec {
    return buildFun("apply") {
      addModifiers(KModifier.ABSTRACT)
      this.addParameter("event", eventType.typeName)
      this.addAnnotation(buildAnnotation(EventSourcingHandler::class) {})
      this.returns(stateClassName)
    }
  }

}
