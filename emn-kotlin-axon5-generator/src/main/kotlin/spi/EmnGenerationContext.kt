package io.holixon.emn.generation.spi

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.EmnAxon5GeneratorProperties
import io.holixon.emn.generation.hasAvroTypeDefinition
import io.holixon.emn.generation.isCommandSlice
import io.holixon.emn.model.*
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordType
import io.toolisticon.kotlin.avro.value.CanonicalName
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
      CommandSlice(slice = it, command = it.flowElements.commands().first(), properties = properties)
    }
  }

  val commands: List<Command> by lazy { slices.map { it.flowElements.commands() }.flatten() }

  val commandTypes: List<CommandType> by lazy { commands.map { it.typeReference as CommandType }.distinct() }

  val events: List<Event> by lazy { slices.map { it.flowElements.filterIsInstance<Event>() }.flatten() }

  val eventTypes: List<EventType> by lazy { commands.map { it.typeReference as EventType }.distinct() }


  override val contextType: KClass<EmnGenerationContext> = EmnGenerationContext::class

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> tag(type: KClass<T>): T? = tags[type] as? T

  val protocolDeclarationContext: ProtocolDeclarationContext get() = tags[ProtocolDeclarationContext::class]!! as ProtocolDeclarationContext

  // FIXME has to be parsed from emn definitions
  val entities: MutableList<Entity> = mutableListOf()

  val avroReferenceTypes: Map<CanonicalName, FlowNodeType> by lazy {
    definitions.nodeTypes.filter { it.hasAvroTypeDefinition() }
      .associateBy { CanonicalName.parse(it.schemaReference()) }
  }

  fun isCommandType(recordType: RecordType): Boolean = avroReferenceTypes[recordType.canonicalName] is CommandType
  fun isEventType(recordType: RecordType): Boolean = avroReferenceTypes[recordType.canonicalName] is EventType
}


