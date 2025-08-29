package io.holixon.emn.generation.spi

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.EmnAxon5GeneratorProperties
import io.holixon.emn.generation.hasAvroTypeDefinitionRef
import io.holixon.emn.generation.isCommandSliceWithAvroTypeDefinitionRef
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
    slices.filter { it.isCommandSliceWithAvroTypeDefinitionRef() }.map {
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
    definitions.nodeTypes.filter { it.hasAvroTypeDefinitionRef() }
      .associateBy { CanonicalName.parse(it.schemaReference()) }
  }

  /**
   * Retrieves an EMN type for given record type.
   * @param recordType Avro Record Type
   * @return Flow Node Type, if defined.
   */
  fun getEmnType(recordType: RecordType): FlowNodeType? = avroReferenceTypes[recordType.canonicalName]

  /**
   * Checks if the provided record reflects an EMN Command type.
   * @return true, if the provided record is generated from EMN command type.
   */
  fun isCommandType(recordType: RecordType): Boolean = getEmnType(recordType) is CommandType
  /**
   * Checks if the provided record reflects an EMN Event type.
   * @return true, if the provided record is generated from EMN event type.
   */
  fun isEventType(recordType: RecordType): Boolean = getEmnType(recordType) is EventType
  /**
   * Checks if the provided record reflects an EMN Query type.
   * @return true, if the provided record is generated from EMN query type.
   */
  fun isQueryType(recordType: RecordType): Boolean = getEmnType(recordType) is QueryType
}


