package io.holixon.emn.generation.spi

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.EmnAxon5GeneratorProperties
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

  val commands: List<Command> by lazy { slices.map { it.flowElements.filterIsInstance<Command>() }.flatten() }

  val commandTypes: List<CommandType> by lazy { commands.map { it.typeReference as CommandType }.distinct() }

  val events: List<Event> by lazy { slices.map { it.flowElements.filterIsInstance<Event>() }.flatten() }

  val eventTypes: List<EventType> by lazy { commands.map { it.typeReference as EventType }.distinct() }

  override val contextType: KClass<EmnGenerationContext> = EmnGenerationContext::class
}
