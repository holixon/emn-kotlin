package io.holixon.emn.generation.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class EmnJsonSchemaHintExtractor(
  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
) {

  fun extract(schema: EmnSchemaDefinition): List<EmnSchemaPropertyHint> {
    val schemaNode = objectMapper.readTree(schema.schemaContent)
    return schemaNode.propertySchemas().mapNotNull { (propertyName, propertySchema) ->
      val entityId = propertySchema["x-emn-role"]?.textValue() == "entity-id"
      val tagKey = propertySchema["x-emn-tag-key"]?.textValue()
      if (!entityId && tagKey == null) {
        null
      } else {
        EmnSchemaPropertyHint(
          propertyName = propertyName,
          entityId = entityId,
          tagKey = tagKey
        )
      }
    }
  }

  private fun JsonNode.propertySchemas(): List<Pair<String, JsonNode>> {
    val result = mutableListOf<Pair<String, JsonNode>>()
    this["properties"]?.takeIf { it.isObject }?.properties()?.forEach { (name, propertySchema) ->
      result.add(name to propertySchema)
    }
    listOf("allOf", "oneOf", "anyOf").forEach { keyword ->
      this[keyword]?.takeIf { it.isArray }?.forEach { child ->
        if (!child.has("\$ref")) {
          result.addAll(child.propertySchemas())
        }
      }
    }
    this["items"]?.takeUnless { it.has("\$ref") }?.let { result.addAll(it.propertySchemas()) }
    return result
  }
}
