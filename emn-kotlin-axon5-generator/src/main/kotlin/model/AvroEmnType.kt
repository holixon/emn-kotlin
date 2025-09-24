package io.holixon.emn.generation.model

sealed interface AvroEmnType
data class AvroEmnCommandType(val id: String) : AvroEmnType
data class AvroEmnEventType(val id: String) : AvroEmnType
data class AvroEmnErrorType(val id: String) : AvroEmnType
data class AvroEmnQueryType(val id: String) : AvroEmnType


@JvmInline
value class AvroEmnTypes(val references: List<AvroEmnType>) {
  constructor(vararg references: AvroEmnType) : this(references.toList())

  val queries : List<AvroEmnQueryType> get() = references.filterIsInstance<AvroEmnQueryType>()
  val events : List<AvroEmnEventType> get() = references.filterIsInstance<AvroEmnEventType>()
  val commands : List<AvroEmnCommandType> get() = references.filterIsInstance<AvroEmnCommandType>()
  val errors : List<AvroEmnErrorType> get() = references.filterIsInstance<AvroEmnErrorType>()
}
