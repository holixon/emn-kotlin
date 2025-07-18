package io.holixon.emn.generation.spi

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.EmnAxon5GeneratorProperties
import io.holixon.emn.generation.commands
import io.holixon.emn.generation.events
import io.holixon.emn.generation.isCommandSlice
import io.holixon.emn.model.Definitions
import io.holixon.emn.model.FlowElement.FlowNode.Command
import io.holixon.emn.model.FlowElement.FlowNode.Event
import io.holixon.emn.model.FlowElementType.FlowNodeType.CommandType
import io.holixon.emn.model.FlowElementType.FlowNodeType.EventType
import io.holixon.emn.model.Lane
import io.holixon.emn.model.Slice
import io.holixon.emn.model.Timeline
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
  registry: EmnAxon5GenerationSpiRegistry,
  val tags: MutableMap<KClass<*>, Any?> = mutableMapOf()
) : KotlinCodeGenerationContextBase<EmnGenerationContext>(registry) {

  val timelines: List<Timeline> by lazy {
    definitions.timelines
  }

  val rootPackageName: PackageName = properties.rootPackageName

  val slices: List<Slice> by lazy { timelines.map { it.sliceSet }.flatten() }

  val commandSlices: List<CommandSlice> by lazy {
    slices.filter { it.isCommandSlice() }.map {
      CommandSlice(slice = it, command = it.flowElements.commands().first())
    }
  }

  val commands: List<Command> by lazy { slices.map { it.flowElements.commands() }.flatten() }

  val commandTypes: List<CommandType> by lazy { commands.map { it.typeReference as CommandType }.distinct() }

  val events: List<Event> by lazy { slices.map { it.flowElements.filterIsInstance<Event>() }.flatten() }

  val eventTypes: List<EventType> by lazy { commands.map { it.typeReference as EventType }.distinct() }

  fun timelines(event: Event): List<Timeline> {
    return timelines.filter { it.flowElements.events().contains(event) }
  }

  fun aggregates(event: Event): List<Lane.AggregateLane> {
    return timelines(event).flatMap { it.laneSet.aggregateLaneSet.filter { it.flowElements.events().contains(event) } }
  }

  override val contextType: KClass<EmnGenerationContext> = EmnGenerationContext::class

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> tag(type: KClass<T>): T? = tags[type] as? T

  val protocolDeclarationContext: ProtocolDeclarationContext get() = tags[ProtocolDeclarationContext::class]!! as ProtocolDeclarationContext

}


