package io.holixon.emn.model

interface WithSchema {
  val schema: Schema?
}

sealed class Schema(open val schemaFormat: String)

data class SchemaDefinition(
  val id: String,
  val name: String,
  override val schema: Schema?
) : WithSchema

data class EmbeddedSchema(override val schemaFormat: String, val content: String) : Schema(schemaFormat = schemaFormat) {
  override fun toString(): String = "content: \'${content}\'"
}

data class ResourceSchema(override val schemaFormat: String, val resource: String) : Schema(schemaFormat = schemaFormat) {
  override fun toString(): String = "resource: $resource"
}

data class ReferencedSchema(override val schemaFormat: String, val reference: String) : Schema(schemaFormat = schemaFormat) {
  override fun toString(): String = "reference: $reference"
}
