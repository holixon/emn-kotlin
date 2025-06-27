package io.holixon.emn.generation.spi

import _ktx.StringKtx.firstUppercase
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.*
import io.holixon.emn.model.*
import io.holixon.emn.model.FlowElement.FlowNode.Command
import io.holixon.emn.model.FlowElement.FlowNode.Event
import io.holixon.emn.model.FlowElementType.FlowNodeType.CommandType
import io.holixon.emn.model.FlowElementType.FlowNodeType.EventType
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.generation.PackageName
import io.toolisticon.kotlin.generation.spi.context.KotlinCodeGenerationContextBase
import kotlin.reflect.KClass

/**
 * Factory to create generation context.
 */
@OptIn(ExperimentalKotlinPoetApi::class)
class EmnGenerationContext(
  val definitions: Definitions,
  val properties: EmnAxon5GeneratorProperties,
  val protocolDeclarationContext: ProtocolDeclarationContext,
  registry: EmnAxon5GenerationSpiRegistry
) : KotlinCodeGenerationContextBase<EmnGenerationContext>(registry) {

  val timelines: List<Timeline> by lazy {
    definitions.timelines
  }

  val rootPackageName: PackageName = properties.rootPackageName

  val slices: List<Slice> by lazy { timelines.map { it.sliceSet }.flatten() }

  val commandSlices: List<CommandSlice> by lazy {
    slices.filter {
      val sliceCommands = it.flowElements.commands() // contain exactly one command
      sliceCommands.size == 1
        && sliceCommands.first().hasAvroTypeDefinition()
        && it.flowElements.events().containsAll(sliceCommands.first().possibleEvents()) // all events are in the slice
    }.map {
      CommandSlice(it, it.flowElements.commands().first())
    }
  }

  val commands: List<Command> by lazy { slices.map { it.flowElements.commands() }.flatten() }

  val commandTypes: List<CommandType> by lazy { commands.map { it.typeReference as CommandType }.distinct() }

  val events: List<Event> by lazy { slices.map { it.flowElements.filterIsInstance<Event>() }.flatten() }

  val eventTypes: List<EventType> by lazy { commands.map { it.typeReference as EventType }.distinct() }

  fun sourcedEvents(command: Command): List<Event> {
    // FIXME -> should be build as reducer
    val directEvents = command.views()
      .flatMap { view -> view.queries() }
      .map { query -> query.events() }
      .flatten()
    return directEvents + directEvents
      .filterNot { directEvents.contains(it)}
      .map { event -> event.commands().filterNot { it == command }
        .map { sourcedEvents(it)  }.flatten()
      }.flatten()
  }

  override val contextType: KClass<EmnGenerationContext> = EmnGenerationContext::class


}


