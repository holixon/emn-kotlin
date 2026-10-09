package io.holixon.emn.generation.schema

import io.holixon.emn.model.CommandType
import io.holixon.emn.model.Definitions
import io.holixon.emn.model.EmbeddedSchema
import io.holixon.emn.model.EventType
import io.holixon.emn.model.FlowNodeType
import io.holixon.emn.model.QueryType
import io.holixon.emn.model.SchemaDefinition

class EmnJsonSchemaExtractor(
  private val acceptedSchemaFormats: Set<String> = DEFAULT_JSON_SCHEMA_FORMATS
) {

  companion object {
    val DEFAULT_JSON_SCHEMA_FORMATS = setOf(
      "application/schema+json",
      "application/json",
      "JSONSchema"
    )
  }

  fun extract(definitions: Definitions, source: String? = null): List<EmnSchemaDefinition> =
    definitions.schemaDefinitions.mapNotNull { schemaDefinition ->
      schemaDefinition.toEmnSchemaDefinition(source)
    } + definitions.nodeTypes.mapNotNull { nodeType ->
      val kind = nodeType.schemaTypeKind() ?: return@mapNotNull null
      val schema = nodeType.schema as? EmbeddedSchema ?: return@mapNotNull null
      if (schema.schemaFormat !in acceptedSchemaFormats) {
        return@mapNotNull null
      }
      EmnSchemaDefinition(
        typeId = nodeType.id,
        typeKind = kind,
        schemaFormat = schema.schemaFormat,
        schemaContent = schema.content,
        source = source
      )
    }

  private fun FlowNodeType.schemaTypeKind(): EmnSchemaTypeKind? =
    when (this) {
      is CommandType -> EmnSchemaTypeKind.COMMAND
      is EventType -> EmnSchemaTypeKind.EVENT
      is QueryType -> EmnSchemaTypeKind.QUERY
      else -> null
    }

  private fun SchemaDefinition.toEmnSchemaDefinition(source: String?): EmnSchemaDefinition? {
    val schema = this.schema as? EmbeddedSchema ?: return null
    if (schema.schemaFormat !in acceptedSchemaFormats) {
      return null
    }
    return EmnSchemaDefinition(
      typeId = id,
      typeKind = EmnSchemaTypeKind.SCHEMA_DEFINITION,
      schemaFormat = schema.schemaFormat,
      schemaContent = schema.content,
      source = source
    )
  }
}
