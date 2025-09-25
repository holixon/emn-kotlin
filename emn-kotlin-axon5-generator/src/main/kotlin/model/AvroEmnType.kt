@file:OptIn(ExperimentalKotlinPoetApi::class)

package io.holixon.emn.generation.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.model.*
import io.toolisticon.kotlin.avro.generator.poet.AvroPoetType
import io.toolisticon.kotlin.avro.model.AvroType
import io.toolisticon.kotlin.avro.value.CanonicalName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.className
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.simpleName
import io.toolisticon.kotlin.avro.model.ErrorType as AvroErrorType
import io.toolisticon.kotlin.avro.model.RecordType as AvroRecordType

sealed interface AvroEmnType<A : AvroType> {
  val avroType: A
  val poetType: AvroPoetType

  val id: String
  val name: String
  val fqn: CanonicalName
}

data class AvroEmnIdType(
  val aggregateLane: AggregateLane,
  override val poetType: AvroPoetType
) : AvroEmnType<AvroRecordType> {
  override val id: String = aggregateLane.id
  override val name: String = simpleName(aggregateLane.name!!)
  override val avroType: AvroRecordType = poetType.avroType as AvroRecordType
  override val fqn: CanonicalName = avroType.canonicalName

  init {
    require(poetType.avroType is AvroRecordType)
  }
}

@OptIn(ExperimentalKotlinPoetApi::class)
sealed interface AvroEmnMessageType<T : FlowNodeType, A : AvroType> : AvroEmnType<A> {
  val nodeType: T
  override val avroType: A
  override val poetType: AvroPoetType

  override val id: String
  override val name: String
  override val fqn: CanonicalName

  val className: ClassName get() = poetType.typeName.className()

  fun idProperty(): String? {
    // FIXME -> find a way how to model this.
    return null
  }
}

data class AvroEmnCommandType(
  override val nodeType: CommandType,
  override val poetType: AvroPoetType,
) : AvroEmnMessageType<CommandType, AvroRecordType> {
  override val id = nodeType.id
  override val name = nodeType.name
  override val fqn: CanonicalName = CanonicalName.parse(nodeType.schemaReference())
  override val avroType: AvroRecordType = poetType.avroType as AvroRecordType

  init {
    require(poetType.avroType is AvroRecordType)
  }
}

data class AvroEmnEventType(
  override val nodeType: EventType,
  override val poetType: AvroPoetType,
) : AvroEmnMessageType<EventType, AvroRecordType> {
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
) : AvroEmnMessageType<ErrorType, AvroErrorType> {
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

  ) : AvroEmnMessageType<QueryType, AvroRecordType> {
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
value class AvroEmnTypes(private val values: List<AvroEmnType<*>>) : List<AvroEmnType<*>> by values {
  constructor(vararg references: AvroEmnMessageType<*, *>) : this(references.toList())

  val messages: List<AvroEmnMessageType<*, *>> get() = values.filterIsInstance<AvroEmnMessageType<*, *>>()
  val queries: List<AvroEmnQueryType> get() = values.filterIsInstance<AvroEmnQueryType>()
  val events: List<AvroEmnEventType> get() = values.filterIsInstance<AvroEmnEventType>()
  val commands: List<AvroEmnCommandType> get() = values.filterIsInstance<AvroEmnCommandType>()
  val errors: List<AvroEmnErrorType> get() = values.filterIsInstance<AvroEmnErrorType>()
  val ids: List<AvroEmnIdType> get() = values.filterIsInstance<AvroEmnIdType>()

  operator fun get(nodeType: CommandType): AvroEmnCommandType = commands.single { it.nodeType.id == nodeType.id }
  operator fun get(nodeType: EventType): AvroEmnEventType = events.single { it.nodeType.id == nodeType.id }
  operator fun get(nodeType: ErrorType): AvroEmnErrorType = errors.single { it.nodeType.id == nodeType.id }
  operator fun get(nodeType: QueryType): AvroEmnQueryType = queries.single { it.nodeType.id == nodeType.id }
  operator fun get(aggregateLane: AggregateLane): AvroEmnIdType = ids.single { it.id == aggregateLane.id }
}
