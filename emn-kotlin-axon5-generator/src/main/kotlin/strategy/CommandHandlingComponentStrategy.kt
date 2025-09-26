package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.generation.*
import io.holixon.emn.generation.model.CommandSlice
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.AggregateLane
import io.holixon.emn.model.applyIfExactlyOne
import io.toolisticon.kotlin.avro.generator.AvroKotlinGenerator
import io.toolisticon.kotlin.avro.generator.poet.AvroPoetType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildClass
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFile
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFun
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildInterface
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildParameter
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.classBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.constructorBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.simpleName
import io.toolisticon.kotlin.generation.spec.KotlinFileSpec
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spec.KotlinFunSpec
import io.toolisticon.kotlin.generation.spec.KotlinInterfaceSpec
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy
import io.toolisticon.kotlin.generation.support.GeneratedAnnotation
import org.axonframework.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalKotlinPoetApi::class)
class CommandHandlingComponentStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, CommandSlice>(
  contextType = EmnGenerationContext::class, inputType = CommandSlice::class
) {

  data class EventTypesToHandle(val sourcingEventTypes: List<AvroPoetType>, val possibleEventTypes: List<AvroPoetType>) :
    Iterable<Pair<AvroPoetType, Boolean>> {
    override fun iterator(): Iterator<Pair<AvroPoetType, Boolean>> = (sourcingEventTypes + possibleEventTypes).distinct().map {
      it to !possibleEventTypes.contains(it)
    }.iterator()
  }

  override fun invoke(context: EmnGenerationContext, input: CommandSlice): KotlinFileSpecList {

    val fileBuilder = fileBuilder(input.commandHandlerClassName)
    fileBuilder.addAnnotation(GeneratedAnnotation(value = AvroKotlinGenerator.NAME).date(context.properties.nowSupplier()))

    val concreteStateTypes = mutableListOf<KotlinFileSpec>()

    val commandHandlerTypeBuilder = classBuilder(input.commandHandlerClassName).apply {
      val command = input.command
      val commandPoetType = context.avroTypes[command.commandType].poetType

      val sourcingEvents = command.sourcingEvents().distinct()
      val possibleEvents = command.possibleEvents().distinct()

      val eventTypesToHandle = EventTypesToHandle(
        sourcingEventTypes = sourcingEvents
          .map { context.avroTypes[it.eventType].poetType }
          .distinct(),
        possibleEventTypes = possibleEvents
          .map { context.avroTypes[it.eventType].poetType }
          .distinct())

      // FIXME -> this is wrong, we should resolve it the same way, it is resolved for generation of the
      // @TargetEntityId in the command
      val idProperty = context.avroTypes[input.command.commandType].idProperty()

      fun buildConcreteState(stateImplClassName: ClassName, stateClassName: ClassName): KotlinFileSpec = buildFile(stateImplClassName) {
        addType(buildClass(stateImplClassName) {
          addSuperinterface(stateClassName)
          primaryConstructor(constructorBuilder().apply {
            addAnnotation(buildAnnotation(EntityCreator::class))
          })
          addFunction(buildFun("decide") {
            addModifiers(KModifier.OVERRIDE)
            addParameter("command", commandPoetType.typeName)
            returns(List::class.asClassName().parameterizedBy(Any::class.asClassName()))
            addStatement("return TODO(\"Not yet implemented\")")
          })
          eventTypesToHandle.forEach { (eventType, noop) ->
            // --
            addFunction(buildFun("evolve") {
              addModifiers(KModifier.OVERRIDE)
              addParameter("event", eventType.typeName)
              returns(stateClassName)
              if (noop) {
                addStatement("return super.evolve(event)")
              } else {
                addStatement("return TODO(\"Not yet implemented\")")
              }
            })
            // --
          }
        })
      }

      fun buildCommandHandler(aggregateLane: AggregateLane) {
        val stateClassName = ClassName(input.commandHandlerClassName.packageName, input.commandHandlerClassName.simpleName, "State")
        val stateImplClassName = ClassName(input.commandHandlerClassName.packageName, "${commandPoetType.typeName.simpleName}State")

        if (context.properties.generateConcreteStateImpl) {
          concreteStateTypes.add(buildConcreteState(stateImplClassName, stateClassName))
        }

        val tagMember = context.resolveAggregateTagName(aggregateLane)

        val state = buildSingleTagState(
          stateClassName,
          stateImplClassName,
          eventTypesToHandle = eventTypesToHandle,
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


      // gather aggregates for all events relevant to this command included in the slice
      val aggregateLanes = (sourcingEvents + possibleEvents).distinct()
        .filter { e -> input.slice.containsFlowElement(e) }
        .flatMap { e -> context.definitions.aggregates(e) }
        .distinct() // only once
      aggregateLanes.applyIfExactlyOne(
        logger.noAggregateFoundLogger(command.typeReference),
        logger.conflictingAggregatesFound(command.typeReference) // TODO -> replace with DCB state generation!
      ) { buildCommandHandler(it) }
    }

    return KotlinFileSpecList(
      fileBuilder.addType(
        commandHandlerTypeBuilder.build()
      ).build()
    ) + KotlinFileSpecList(concreteStateTypes)
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
    stateClassName: ClassName,
    stateImplClassName: ClassName,
    eventTypesToHandle: EventTypesToHandle,
    commandType: AvroPoetType,
    tagMember: MemberName
  ): KotlinInterfaceSpec {


    fun evolve(eventType: AvroPoetType, noop: Boolean): KotlinFunSpec = buildFun("evolve") {
      addParameter("event", eventType.typeName)
      addAnnotation(EventSourcingHandlerAnnotation)
      returns(stateClassName)
      if (noop) {
        addStatement("return this")
      } else {
        addModifiers(KModifier.ABSTRACT)
      }
    }

    fun decide(commandType: AvroPoetType): KotlinFunSpec = buildFun("decide") {
      addModifiers(KModifier.ABSTRACT)
      this.addParameter("command", commandType.typeName)
      this.returns(List::class.asClassName().parameterizedBy(Any::class.asClassName()))
    }

    return buildInterface(stateClassName) {
      addAnnotation(EventSourcedEntityAnnotation(tagMember, listOf(stateImplClassName)))

      addFunction(decide(commandType))

      eventTypesToHandle.forEach { (event, noop) ->
        addFunction(evolve(event, noop))
      }
    }
  }

  override fun test(context: EmnGenerationContext, input: Any): Boolean {
    return context.properties.generateCommandSlices && super.test(context, input)
  }
}
