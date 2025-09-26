package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.generation.*
import io.holixon.emn.generation.model.CommandSlice
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.AggregateLane
import io.holixon.emn.model.Command
import io.holixon.emn.model.Event
import io.holixon.emn.model.applyIfExactlyOne
import io.toolisticon.kotlin.avro.generator.AvroKotlinGenerator
import io.toolisticon.kotlin.avro.generator.poet.AvroPoetType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFun
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildParameter
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.classBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.constructorBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.className
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.simpleName
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy
import io.toolisticon.kotlin.generation.support.GeneratedAnnotation
import org.axonframework.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.annotations.reflection.EntityCreator

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalKotlinPoetApi::class)
class CommandHandlingComponentStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, CommandSlice>(
  contextType = EmnGenerationContext::class, inputType = CommandSlice::class
) {

  data class EventTypesToHandle(
    val sourcingEvents: List<Event>,
    val possibleEvents: List<Event>,
    val sourcingEventTypes: List<AvroPoetType>,
    val possibleEventTypes: List<AvroPoetType>,
    val aggregateLanes: List<AggregateLane>
  ) : Iterable<Pair<AvroPoetType, Boolean>> {
    companion object {
      operator fun invoke(command: Command, commandSlice: CommandSlice, context: EmnGenerationContext): EventTypesToHandle {
        val sourcingEvents = command.sourcingEvents().distinct()
        val possibleEvents = command.possibleEvents().distinct()

        return EventTypesToHandle(
          sourcingEvents = sourcingEvents,
          possibleEvents = possibleEvents,
          sourcingEventTypes = sourcingEvents
            .map { context.avroTypes[it.eventType].poetType }
            .distinct(),
          possibleEventTypes = possibleEvents
            .map { context.avroTypes[it.eventType].poetType }
            .distinct(),
          // gather aggregates for all events relevant to this command included in the slice
          aggregateLanes = (sourcingEvents + possibleEvents).distinct()
            .filter { e -> commandSlice.slice.containsFlowElement(e) }
            .flatMap { e -> context.definitions.aggregates(e) }
            .distinct()
        )
      }
    }

    override fun iterator(): Iterator<Pair<AvroPoetType, Boolean>> = (sourcingEventTypes + possibleEventTypes).distinct().map {
      it to !possibleEventTypes.contains(it)
    }.iterator()
  }


  override fun invoke(context: EmnGenerationContext, input: CommandSlice): KotlinFileSpecList {

    val command = input.command
    val commandPoetType = context.avroTypes[command.commandType].poetType
    val commandHandlerClassName = input.commandHandlerClassName
    val stateClassName = ClassName(input.commandHandlerClassName.packageName, "${commandPoetType.typeName.simpleName}State")

    val commandHandlerFile = fileBuilder(commandHandlerClassName)
      .addAnnotation(GeneratedAnnotation(value = AvroKotlinGenerator.NAME).date(context.properties.nowSupplier()))
    val stateFile = fileBuilder(stateClassName)
      .addAnnotation(GeneratedAnnotation(value = AvroKotlinGenerator.NAME).date(context.properties.nowSupplier()))

    val commandHandlerClass = classBuilder(commandHandlerClassName).apply {
      if (context.properties.annotateForSpringBoot) {
        addAnnotation(ClassName("org.springframework.stereotype", "Component"))
      }
    }
    val stateClass = classBuilder(stateClassName)

    val eventTypesToHandle = EventTypesToHandle(command, input, context)

    eventTypesToHandle.aggregateLanes.applyIfExactlyOne(
      logger.noAggregateFoundLogger(command.typeReference),
      logger.conflictingAggregatesFound(command.typeReference) // TODO -> replace with DCB state generation!
    ) {
      // exactly one lane!
        aggregateLane ->

      // FIXME -> this is wrong, we should resolve it the same way, it is resolved for generation of the
      // @TargetEntityId in the command
      val idProperty = context.avroTypes[input.command.commandType].idProperty()
      val tagMember = context.resolveAggregateTagName(aggregateLane)
      val idType = context.avroTypes.ids.single { it.aggregateLane.id == aggregateLane.id }

      // add one handle method to the commandHandler

      // region [CommandHandler]
      commandHandlerClass.apply {
        addFunction(
          buildFun("handle") {
            addAnnotation(CommandHandlerAnnotation)
            this.addParameter("command", commandPoetType.typeName)

            this.addParameter(
              buildParameter("state", stateClassName) {
                addAnnotation(InjectEntityAnnotation(idProperty))
              }
            )

            this.addParameter("eventAppender", EventAppender::class)

            addStatement("eventAppender.append(state.decide(command))")
          }
        )
      }
      // endregion [CommandHandler]

      // region [State]
      stateClass.apply {
        if (context.properties.annotateForSpringBoot) {
          addAnnotation(EventSourcedAnnotation(tagMember, idType.poetType.typeName.className()))
        } else {
          addAnnotation(EventSourcedEntityAnnotation(tagMember))
        }
        primaryConstructor(constructorBuilder().apply {
          addAnnotation(buildAnnotation(EntityCreator::class))
        })
        addFunction(buildFun("decide") {
          addParameter("command", commandPoetType.typeName)
          returns(List::class.asClassName().parameterizedBy(Any::class.asClassName()))
          addStatement("return TODO(\"Not yet implemented\")")
        })
        eventTypesToHandle.forEach { (eventType, noop) ->
          addFunction(buildFun("evolve") {
            addParameter("event", eventType.typeName)
            returns(stateClassName)
            if (noop) {
              addStatement("return apply{}")
            } else {
              addStatement("return TODO(\"Not yet implemented\")")
            }
          })
        }
      }
      // endregion [State]
    }

    return KotlinFileSpecList(
      buildList {
        add(commandHandlerFile.addType(commandHandlerClass).build())
        if (context.properties.generateConcreteStateImpl) {
          add(stateFile.addType(stateClass).build())
        }
      }
    )
  }

  override fun test(context: EmnGenerationContext, input: Any): Boolean {
    return context.properties.generateCommandSlices && super.test(context, input)
  }
}
