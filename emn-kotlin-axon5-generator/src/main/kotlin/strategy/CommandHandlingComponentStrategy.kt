package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.generation.*
import io.holixon.emn.generation.spi.CommandSlice
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.generation.spi.commandHandlerClassName
import io.holixon.emn.model.applyIfExactlyOne
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFun
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildInterface
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildParameter
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.classBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spec.KotlinFunSpec
import io.toolisticon.kotlin.generation.spec.KotlinInterfaceSpec
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy
import org.axonframework.eventhandling.gateway.EventAppender

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalKotlinPoetApi::class)
class CommandHandlingComponentStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, CommandSlice>(
  contextType = EmnGenerationContext::class, inputType = CommandSlice::class
) {
  override fun invoke(context: EmnGenerationContext, input: CommandSlice): KotlinFileSpecList {

    val fileBuilder = fileBuilder(input.commandHandlerClassName)

    val commandHandlerTypeBuilder = classBuilder(input.commandHandlerClassName).apply {

      val command = input.command
      val commandPoetType = command.typeReference.resolveAvroPoetType(context.protocolDeclarationContext)
      val eventsToHandle = (command.sourcingEvents() + command.possibleEvents()).distinct()
      val eventTypesToHandle = eventsToHandle
        .map { it.typeReference.resolveAvroPoetType(context.protocolDeclarationContext) }
        .distinct()

      // FIXME -> this is wrong, we should resolve it the same way, it is resolved for generation of the
      // @TargetEntityId in the command
      val idProperty = input.command.typeReference.resolveAvroPoetType(context.protocolDeclarationContext).idProperty()

      // gather aggregates for all events relevant to this command included in the slice
      val aggregateLanes = eventsToHandle
        .filter { e -> input.slice.containsFlowElement(e) }
        .flatMap { e -> context.definitions.aggregates(e) }
        .distinct() // only once
      aggregateLanes.applyIfExactlyOne(
        logger.noAggregateFoundLogger(command.typeReference),
        logger.conflictingAggregatesFound(command.typeReference) // TODO -> replace with DCB state generation!
      ) { aggregateLane ->

        if (aggregateLane.name != null) {
          val tagMember = context.resolveAggregateTagName(aggregateLane)

          val state = buildSingleTagState(
            handlerClassName = input.commandHandlerClassName,
            sourcingEventTypes = eventTypesToHandle,
            commandType = commandPoetType,
            tagMember = tagMember
          )
          addFunction(
            buildHandler(
              commandType = commandPoetType,
              stateSpec = state,
              idProperty = idProperty
            )
          )
          addType(state)
        }
      }
    }
    fileBuilder.addType(commandHandlerTypeBuilder.build())
    return KotlinFileSpecList(fileBuilder.build())
  }

  private fun buildHandler(
    commandType: AvroPoetType,
    stateSpec: KotlinInterfaceSpec,
    idProperty: String?
  ): KotlinFunSpec {
    return buildFun("handle") {
      addAnnotation(CommandHandlerAnnotation)
      this.addParameter("command", commandType.typeName)

      this.addParameter(
        buildParameter("state", stateSpec.className) {
          addAnnotation(InjectEntityAnnotation(idProperty))
        }
      )

      this.addParameter("eventAppender", EventAppender::class)

      addStatement("eventAppender.append(state.decide(command))")
    }
  }

  private fun buildSingleTagState(
    handlerClassName: ClassName,
    sourcingEventTypes: List<AvroPoetType>,
    commandType: AvroPoetType,
    tagMember: MemberName
  ): KotlinInterfaceSpec {

    val stateClassName = ClassName(handlerClassName.packageName, handlerClassName.simpleName, "State")
    return buildInterface(stateClassName) {
      addAnnotation(EventSourcedEntityAnnotation(tagMember))
      sourcingEventTypes.forEach { event ->
        addFunction(buildEventSourcingHandler(event, stateClassName))
      }
      addFunction(buildCommandDecider(commandType))
    }
  }

  private fun buildCommandDecider(commandType: AvroPoetType) : KotlinFunSpec {
    return buildFun("decide") {
      addModifiers(KModifier.ABSTRACT)
      this.addParameter("command", commandType.typeName)
      this.returns(List::class.asClassName().parameterizedBy(Any::class.asClassName()))
    }

  }

  private fun buildEventSourcingHandler(eventType: AvroPoetType, stateClassName: ClassName): KotlinFunSpec {
    return buildFun("apply") {
      addModifiers(KModifier.ABSTRACT)
      this.addParameter("event", eventType.typeName)
      this.addAnnotation(EventSourcingHandlerAnnotation)
      this.returns(stateClassName)
    }
  }
}
