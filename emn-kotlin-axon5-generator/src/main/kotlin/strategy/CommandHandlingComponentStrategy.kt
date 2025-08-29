package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.generation.*
import io.holixon.emn.generation.ext.StringTransformations
import io.holixon.emn.generation.ext.StringTransformations.TO_UPPER_SNAKE_CASE
import io.holixon.emn.generation.spi.CommandSlice
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.generation.spi.commandHandlerClassName
import io.holixon.emn.model.applyIfExactlyOne
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFun
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildInterface
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildParameter
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.classBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.spec.*
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy
import org.axonframework.commandhandling.annotation.CommandHandler
import org.axonframework.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventSourcedEntity

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalKotlinPoetApi::class)
class CommandHandlingComponentStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, CommandSlice>(
  contextType = EmnGenerationContext::class, inputType = CommandSlice::class
) {
  override fun invoke(context: EmnGenerationContext, input: CommandSlice): KotlinFileSpecList {

    val handlerName = input.simpleClassName + "CommandHandler"
    val fileBuilder = fileBuilder(input.commandHandlerClassName)

    val commandHandlerTypeBuilder = classBuilder(input.commandHandlerClassName).apply {

      val command = input.command
      val commandPoetTypeType = command.typeReference.resolveAvroPoetType(context.protocolDeclarationContext)
      val sourcingEvents = command.sourcedEvents()
      val sourcingEventTypes =
        sourcingEvents.map { it.typeReference.resolveAvroPoetType(context.protocolDeclarationContext) }

      val idProperty = if (sourcingEvents.isEmpty()) { // no source events
        input.command.typeReference.resolveAvroPoetType(context.protocolDeclarationContext).idProperty()
      } else {
        null
      }

      val aggregateLanes = sourcingEvents
        .filter { e ->  input.slice.containsFlowElement(e) } // we are generating per-slice
        .flatMap { e -> context.definitions.aggregates(e) }
        .distinct() // only once
      aggregateLanes.applyIfExactlyOne(
        logger.noAggregateFoundLogger(command.typeReference),
        logger.conflictingAggregatesFound(command.typeReference) // TODO -> replace with DCB state generation!
      ) { aggregateLane ->

        val aggregateName = aggregateLane.name
        if (aggregateName != null) {
          val tagClassName = KotlinCodeGeneration.className(
            packageName = context.properties.rootPackageName,
            simpleName = StringTransformations.TO_UPPER_CAMEL_CASE(context.properties.emnName + "Tags")
          )
          val tagMember = MemberName(tagClassName, TO_UPPER_SNAKE_CASE(aggregateName))

          val state = buildStateSingleAggregate(
            handlerClassName = input.commandHandlerClassName,
            sourcingEventTypes = sourcingEventTypes,
            tagMember = tagMember
          )
          addFunction(
            buildHandler(
              commandType = commandPoetTypeType,
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

  private fun buildStateSingleAggregate(
    slicePackage: String,
    handlerName: String,
    sourcingEventTypes: List<AvroPoetType>
  ): KotlinInterfaceSpec {
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

  private fun buildStateSingleAggregate(
    handlerClassName: ClassName,
    sourcingEventTypes: List<AvroPoetType>,
    tagMember: MemberName
  ): KotlinInterfaceSpec {

    val stateClassName = ClassName(handlerClassName.packageName, handlerClassName.simpleName, "State")
    return buildInterface(stateClassName) {
      addAnnotation(EventSourcedEntityAnnotation(tagMember))
      sourcingEventTypes.forEach { event ->
        addFunction(buildEventSourcingHandler(event, stateClassName))
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

  /**
   * @EventSourcedEntity(
   *     tagKey = FacultyTags.COURSE_ID
   * )
   */
  @OptIn(ExperimentalKotlinPoetApi::class)
  data class EventSourcedEntityAnnotation(val key: MemberName) : KotlinAnnotationSpecSupplier {
    override fun spec(): KotlinAnnotationSpec = buildAnnotation(EventSourcedEntity::class) {
      addMember("tagKey = %M", key)
    }
  }

}
