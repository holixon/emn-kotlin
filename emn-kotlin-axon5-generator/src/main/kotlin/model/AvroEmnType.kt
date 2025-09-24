package io.holixon.emn.generation.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.model.*
import io.toolisticon.kotlin.avro.generator.poet.AvroPoetType
import io.toolisticon.kotlin.avro.model.AvroType
import io.toolisticon.kotlin.avro.value.CanonicalName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.className
import io.toolisticon.kotlin.avro.model.ErrorType as AvroErrorType
import io.toolisticon.kotlin.avro.model.RecordType as AvroRecordType

@OptIn(ExperimentalKotlinPoetApi::class)
sealed interface AvroEmnType<T : FlowNodeType, A : AvroType> {
  val nodeType: T
  val avroType: A
  val poetType: AvroPoetType

  val id: String
  val name: String
  val fqn: CanonicalName

  val className: ClassName get() = poetType.typeName.className()

  fun idProperty(): String? {
    // FIXME -> find a way how to model this.
    return null
  }
}

data class AvroEmnCommandType(
  override val nodeType: CommandType,
  override val poetType: AvroPoetType,
) : AvroEmnType<CommandType, AvroRecordType> {
  override val id = nodeType.id
  override val name = nodeType.name
  override val fqn: CanonicalName = CanonicalName.parse(nodeType.schemaReference())
  override val avroType: AvroRecordType
    get() = poetType.avroType as AvroRecordType

  init {
    require(poetType.avroType is AvroRecordType)
  }
}

data class AvroEmnEventType(
  override val nodeType: EventType,
  override val poetType: AvroPoetType,
) : AvroEmnType<EventType, AvroRecordType> {
  override val id = nodeType.id
  override val name = nodeType.name
  override val fqn: CanonicalName = CanonicalName.parse(nodeType.schemaReference())
  override val avroType: AvroRecordType = poetType.avroType as AvroRecordType

  init {
    require(poetType.avroType is AvroRecordType)
  }
}

data class AvroEmnErrorType(
  override val nodeType: ErrorType,
  override val poetType: AvroPoetType,
) : AvroEmnType<ErrorType, AvroErrorType> {
  override val id = nodeType.id
  override val name = nodeType.name
  override val fqn: CanonicalName = CanonicalName.parse(nodeType.schemaReference())
  override val avroType: AvroErrorType = poetType.avroType as AvroErrorType

  init {
    require(poetType.avroType is AvroErrorType)
  }
}

data class AvroEmnQueryType(
  override val nodeType: QueryType, override val poetType: AvroPoetType,

  ) : AvroEmnType<QueryType, AvroRecordType> {
  override val id = nodeType.id
  override val name = nodeType.name
  override val fqn: CanonicalName = CanonicalName.parse(nodeType.schemaReference())
  override val avroType: AvroRecordType = poetType.avroType as AvroRecordType

  init {
    require(poetType.avroType is AvroRecordType)
  }
}

@JvmInline
@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
value class AvroEmnTypes(private val values: List<AvroEmnType<*, *>>) : List<AvroEmnType<*, *>> by values {
  constructor(vararg references: AvroEmnType<*, *>) : this(references.toList())

  val queries: List<AvroEmnQueryType> get() = values.filterIsInstance<AvroEmnQueryType>()
  val events: List<AvroEmnEventType> get() = values.filterIsInstance<AvroEmnEventType>()
  val commands: List<AvroEmnCommandType> get() = values.filterIsInstance<AvroEmnCommandType>()
  val errors: List<AvroEmnErrorType> get() = values.filterIsInstance<AvroEmnErrorType>()

  operator fun get(nodeType: CommandType): AvroEmnCommandType = commands.single { it.nodeType.id == nodeType.id }
  operator fun get(nodeType: EventType): AvroEmnEventType = events.single { it.nodeType.id == nodeType.id }
  operator fun get(nodeType: ErrorType): AvroEmnErrorType = errors.single { it.nodeType.id == nodeType.id }
  operator fun get(nodeType: QueryType): AvroEmnQueryType = queries.single { it.nodeType.id == nodeType.id }
}
