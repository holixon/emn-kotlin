package io.holixon.emn.generation.schema

enum class EmnSchemaTypeKind {
  COMMAND,
  EVENT,
  QUERY,
  SCHEMA_DEFINITION
}

data class EmnSchemaDefinition(
  val typeId: String,
  val typeKind: EmnSchemaTypeKind,
  val schemaFormat: String,
  val schemaContent: String,
  val source: String? = null
)
