package io.holixon.emn.generation.spi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.MemberName
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator
import io.holixon.emn.generation.EmnAxon5GeneratorProperties
import io.holixon.emn.generation.hasAvroTypeDefinitionRef
import io.holixon.emn.generation.isCommandSliceWithAvroTypeDefinitionRef
import io.holixon.emn.generation.model.*
import io.holixon.emn.model.*
import io.konform.validation.Validation
import io.toolisticon.kotlin.avro.declaration.ProtocolDeclaration
import io.toolisticon.kotlin.avro.generator.spi.AvroCodeGenerationSpiRegistry
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.model.AvroNamedType
import io.toolisticon.kotlin.avro.model.RecordType
import io.toolisticon.kotlin.avro.value.CanonicalName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.className
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.constantName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.simpleName
import io.toolisticon.kotlin.generation.PackageName
import io.toolisticon.kotlin.generation.spi.context.KotlinCodeGenerationContextBase
import io.toolisticon.kotlin.generation.spi.registry.KotlinCodeGenerationSpiList
import io.toolisticon.kotlin.generation.support.GeneratedAnnotation
import kotlin.reflect.KClass

/**
 * Factory to create generation context.
 */
@OptIn(ExperimentalKotlinPoetApi::class)
class EmnGenerationContext(
  registry: EmnAxon5GenerationSpiRegistry,
  val definitions: Definitions,
  val properties: EmnAxon5GeneratorProperties,
  val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule(),
  /**
   * Tags can be used by kotlinPoet to add additional data at runtime.
   */
  val tags: MutableMap<KClass<*>, Any?> = mutableMapOf()
) : KotlinCodeGenerationContextBase<EmnGenerationContext>(registry) {

  companion object {
    val validateContext = Validation<EmnGenerationContext> {
      dynamic { ctx ->
        // Checks that all referenced Avro types are actually declared in the used protocol.
        ctx.definitions.nodeTypes.filter { it.hasAvroTypeDefinitionRef() }
          .map { it.id to CanonicalName.parse(it.schemaReference()) }
          .forEach { (id, fqn) ->
            constrain("NodeType '$id' references Avro type '${fqn.fqn}', but it is not declared in protocol '${ctx.protocolDeclarationContext.protocol.canonicalName.fqn}'") {
              ctx.protocolTypesByFqn.containsKey(fqn)
            }
          }

        val aggregateIdSchemaRefs = ctx.definitions.aggregates().filter { it.hasAvroTypeDefinitionRef() }
          .map { it.id to CanonicalName.parse(it.schemaReference()) }

        // Checks that all referenced id types are actually declared in the used protocol.
        aggregateIdSchemaRefs.forEach { (id, fqn) ->
          constrain("Aggregate '$id' references Avro id type '${fqn.fqn}', but it is not declared in protocol '${ctx.protocolDeclarationContext.protocol.canonicalName.fqn}'") {
            ctx.protocolTypesByFqn.containsKey(fqn)
          }
        }

        // checks if all id types are value types (single property with name='value')
        aggregateIdSchemaRefs.map { (id, fqn) -> id to ctx.protocolTypesByFqn[fqn]!! }
          .forEach { (id, schemaType) ->
            constrain("Aggregate '$id' references Avro id type '${schemaType.schema.canonicalName.fqn}', but it is not a valid id-type (single property with name='value'), was=${(schemaType as RecordType).fields.map { it.name }}.") {
              schemaType.fields.size == 1 && schemaType.fields[0].name.value == "value"
            }
          }

        // checks if all error types are valid (single property with name='message')
        ctx.protocolDeclarationContext.avroTypes.values.filterIsInstance<io.toolisticon.kotlin.avro.model.ErrorType>().forEach { error ->
          constrain("Exceptions declared in avro protocol must have exactly one string field name='message', but '${error.canonicalName.fqn}' has fields ${error.fields.map { f -> f.name }}.") {
            error.fields.size == 1 && error.fields[0].name.value == "message"
          }
        }
      }

      EmnGenerationContext::definitions {

        Definitions::specifications onEach {
          run(Specification.validateParsedSpecification)
        }
        dynamic { definitions ->
          definitions.aggregates().forEach { aggregateLane ->
            constrain("AggregateLane must have a name, was null for id '${aggregateLane.id}'") {
              !aggregateLane.name.isNullOrBlank()
            }
          }
        }
      }

    }

    fun create(
      declaration: ProtocolDeclaration,
      definitions: Definitions,
      spiList: KotlinCodeGenerationSpiList,
      properties: EmnAxon5GeneratorProperties,
    ) = create(declaration, definitions, EmnAxon5GenerationSpiRegistry(spiList), AvroCodeGenerationSpiRegistry(spiList), properties)

    fun create(
      declaration: ProtocolDeclaration,
      definitions: Definitions,
      registry: EmnAxon5GenerationSpiRegistry,
      avroRegistry: AvroCodeGenerationSpiRegistry,
      properties: EmnAxon5GeneratorProperties,
    ): EmnGenerationContext {
      val emnCtx = EmnGenerationContext(
        definitions = definitions,
        registry = registry,
        properties = properties
      )

      val avprContext = ProtocolDeclarationContext.of(
        declaration = declaration,
        registry = avroRegistry,
        properties = properties
      )

      avprContext.tags[EmnGenerationContext::class] = emnCtx
      emnCtx.tags[ProtocolDeclarationContext::class] = avprContext

      return emnCtx
    }
  }

  val protocolDeclarationContext by lazy {
    checkNotNull(tag(ProtocolDeclarationContext::class)) { "ProtocolDeclarationContext not found in tags, this is a misconfiguration." }
  }

  val protocolTypesByFqn: Map<CanonicalName, AvroNamedType> by lazy {
    protocolDeclarationContext.avroTypes.values
      .filterIsInstance<AvroNamedType>()
      .associateBy { it.schema.canonicalName }
  }

  /**
   * Resolve all EMN types that have an Avro type mapping.
   */
  val avroTypes by lazy {
    val nodeTypes = definitions.nodeTypes.filter { it.hasAvroTypeDefinitionRef() }
      .map {
        it to protocolTypesByFqn[CanonicalName.parse(it.schemaReference())]!!
      }.map {
        it.first to protocolDeclarationContext.avroPoetTypes[it.second.hashCode]
      }
      .map {
        when (it.first) {
          is CommandType -> AvroEmnCommandType(
            nodeType = it.first as CommandType,
            poetType = it.second
          )

          is EventType -> AvroEmnEventType(
            nodeType = it.first as EventType,
            poetType = it.second
          )

          is QueryType -> AvroEmnQueryType(
            nodeType = it.first as QueryType,
            poetType = it.second
          )

          is ErrorType -> AvroEmnErrorType(
            nodeType = it.first as ErrorType,
            poetType = it.second
          )

          else -> throw IllegalStateException("EMN type ${it.first::class.simpleName} is not supported for Avro type mapping")
        }
      }

    val idTypes = definitions.aggregates().filter { it.hasAvroTypeDefinitionRef() }
      .map {
        it to protocolTypesByFqn[CanonicalName.parse(it.schemaReference())]!!
      }
      .map {
        it.first to protocolDeclarationContext.avroPoetTypes[it.second.hashCode]
      }
      .map { AvroEmnIdType(it.first, it.second) }

    AvroEmnTypes(nodeTypes + idTypes)
  }

  val specifications by lazy {
    Specifications(definitions.specifications.map { Specification(it) })
  }

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

  val events: List<Event> by lazy { slices.map { it.flowElements.filterIsInstance<Event>() }.flatten() }

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

  fun resolveAggregateTagName(aggregateLane: AggregateLane): MemberName {
    val aggregateName = requireNotNull(aggregateLane.name) { "Aggregate name must not be blank" }
    return MemberName(getTagClassName(), constantName(aggregateName))
  }

  /**
   * Retrieve class name for Tags class.
   */
  fun getTagClassName(): ClassName {
    return className(
      packageName = properties.rootPackageName,
      simpleName = simpleName(properties.emnName + "Tags")
    )
  }

  val generatedAnnotation by lazy {
    GeneratedAnnotation(value = EmnAxon5AvroBasedGenerator.NAME).date(properties.nowSupplier())
  }

  // region [overrides]
  override val contextType: KClass<EmnGenerationContext> = EmnGenerationContext::class

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> tag(type: KClass<T>): T? = tags[type] as? T
  // endregion [overrides]

}
