package io.holixon.emn.generation.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class EmnJsonSchemaHintValidator(
  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
) {

  fun validate(schemas: List<EmnSchemaDefinition>) {
    schemas.forEach { schema -> validate(schema, schemas) }
  }

  private fun validate(schema: EmnSchemaDefinition, schemas: List<EmnSchemaDefinition>) {
    val schemaNode = objectMapper.readTree(schema.schemaContent)
    schemaNode.propertySchemas().forEach { (propertyName, propertySchema) ->
      propertySchema["x-emn-role"]?.textValue()?.let { role ->
        require(role == "entity-id") {
          "Unsupported x-emn-role '$role' on '${schema.typeId}.$propertyName'."
        }
        require(schema.isUsedOnlyBy(EmnSchemaTypeKind.COMMAND, schemas)) {
          "x-emn-role: entity-id is only supported on command schemas, but was used on ${schema.typeKind} '${schema.typeId}.$propertyName'."
        }
      }

      if (propertySchema.has("x-emn-tag-key")) {
        require(schema.isUsedOnlyBy(EmnSchemaTypeKind.EVENT, schemas)) {
          "x-emn-tag-key is only supported on event schemas, but was used on ${schema.typeKind} '${schema.typeId}.$propertyName'."
        }
        require(propertySchema["x-emn-tag-key"].isTextual && propertySchema["x-emn-tag-key"].textValue().isNotBlank()) {
          "x-emn-tag-key on '${schema.typeId}.$propertyName' must be a non-blank string."
        }
      }
    }
  }

  private fun JsonNode.propertySchemas(): List<Pair<String, JsonNode>> {
    val result = mutableListOf<Pair<String, JsonNode>>()
    this["properties"]?.takeIf { it.isObject }?.properties()?.forEach { (name, schema) ->
      result.add(name to schema)
    }
    listOf("allOf", "oneOf", "anyOf").forEach { keyword ->
      this[keyword]?.takeIf { it.isArray }?.forEach { child ->
        result.addAll(child.propertySchemas())
      }
    }
    this["items"]?.takeUnless { it.has("\$ref") }?.let { result.addAll(it.propertySchemas()) }
    return result
  }

  private fun EmnSchemaDefinition.isUsedOnlyBy(
    expectedKind: EmnSchemaTypeKind,
    schemas: List<EmnSchemaDefinition>
  ): Boolean {
    if (typeKind != EmnSchemaTypeKind.SCHEMA_DEFINITION) {
      return typeKind == expectedKind
    }

    val schemasById = schemas.associateBy { it.typeId }
    val consumers = schemas
      .filter { it.typeKind != EmnSchemaTypeKind.SCHEMA_DEFINITION }
      .filter { it.referencesTransitively(typeId, schemasById, emptySet()) }
    return consumers.isNotEmpty() && consumers.all { it.typeKind == expectedKind }
  }

  private fun EmnSchemaDefinition.referencesTransitively(
    targetTypeId: String,
    schemasById: Map<String, EmnSchemaDefinition>,
    visited: Set<String>
  ): Boolean {
    if (typeId in visited) {
      return false
    }
    return objectMapper.readTree(schemaContent).references().any { reference ->
      reference == targetTypeId || schemasById[reference]?.referencesTransitively(
        targetTypeId = targetTypeId,
        schemasById = schemasById,
        visited = visited + typeId
      ) == true
    }
  }

  private fun JsonNode.references(): Set<String> {
    val references = mutableSetOf<String>()
    if (isObject) {
      this["\$ref"]?.takeIf { it.isTextual }?.textValue()?.removeSuffix(".schema.json")?.let(references::add)
      properties().forEach { (_, child) -> references.addAll(child.references()) }
    } else if (isArray) {
      forEach { child -> references.addAll(child.references()) }
    }
    return references
  }
}
