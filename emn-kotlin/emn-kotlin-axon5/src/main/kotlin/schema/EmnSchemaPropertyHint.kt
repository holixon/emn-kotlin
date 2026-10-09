package io.holixon.emn.generation.schema

data class EmnSchemaPropertyHint(
  val propertyName: String,
  val entityId: Boolean = false,
  val tagKey: String? = null
)
