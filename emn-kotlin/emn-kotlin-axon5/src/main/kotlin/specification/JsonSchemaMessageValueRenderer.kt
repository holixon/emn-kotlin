package io.holixon.emn.generation.specification

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import io.holixon.emn.generation.schema.EmnSchemaDefinition
import io.holixon.emn.generation.schema.EmnSchemaPackageLayout
import io.holixon.emn.generation.schema.KotlinIdentifier
import io.holixon.emn.model.EmbeddedValue
import io.holixon.emn.model.FlowNode

/** Renders a JSON example value as a Kotlin expression using its extracted JSON Schema. */
class JsonSchemaMessageValueRenderer(
  schemas: List<EmnSchemaDefinition>,
  rootPackageName: String,
  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule(),
  private val packageLayout: EmnSchemaPackageLayout = EmnSchemaPackageLayout()
) : MessageValueRenderer {

  private val schemasByTypeId = schemas.associateBy { it.typeId }
  private val schemaNodesByTypeId = schemas.associate { it.typeId to objectMapper.readTree(it.schemaContent) }
  private val packagesByTypeId = schemas.associate { schema ->
    schema.typeId to packageLayout.packageName(rootPackageName, schema.typeKind)
  }

  override fun render(message: FlowNode): CodeBlock {
    val value = requireNotNull(message.value as? EmbeddedValue) {
      "Specification element '${message.id}' must contain an embedded value."
    }
    require(value.valueFormat == "application/json") {
      "Specification element '${message.id}' must use application/json values, but was '${value.valueFormat}'."
    }
    val typeId = message.typeReference.id
    val schema = requireNotNull(schemaNodesByTypeId[typeId]) {
      "No embedded JSON Schema found for specification type '$typeId'."
    }
    return renderValue(objectMapper.readTree(value.content), schema, typeId)
  }

  /** Renders an error value as schema-typed structured details instead of a generated class. */
  fun renderErrorDetails(error: FlowNode): CodeBlock {
    val value = error.value?.let {
      require(it is EmbeddedValue) {
        "Specification error '${error.id}' must contain an embedded value."
      }
      require(it.valueFormat == "application/json") {
        "Specification error '${error.id}' must use application/json values, but was '${it.valueFormat}'."
      }
      objectMapper.readTree(it.content)
    } ?: objectMapper.createObjectNode()
    val typeId = error.typeReference.id
    val schema = requireNotNull(schemaNodesByTypeId[typeId]) {
      "No embedded JSON Schema found for specification error type '$typeId'."
    }
    require(value.isObject) { "Expected an object value for specification error type '$typeId'." }
    return renderStructuredValue(value, schema)
  }

  fun className(typeId: String): ClassName {
    val schema = requireNotNull(schemasByTypeId[typeId]) {
      "No embedded JSON Schema found for specification type '$typeId'."
    }
    return ClassName(
      packagesByTypeId.getValue(typeId),
      KotlinIdentifier.requireClassName(schema.typeId, "class name from TypeId")
    )
  }

  private fun renderValue(value: JsonNode, schema: JsonNode, declaredTypeId: String? = null): CodeBlock {
    if (value.isNull) {
      return CodeBlock.of("null")
    }

    schema["\$ref"]?.takeIf(JsonNode::isTextual)?.textValue()?.let { reference ->
      val referencedTypeId = reference.localTypeId()
      val referencedSchema = requireNotNull(schemaNodesByTypeId[referencedTypeId]) {
        "Schema references unknown local schema '$referencedTypeId'."
      }
      return renderValue(value, referencedSchema, referencedTypeId)
    }

    schema["const"]?.let { constant ->
      return renderValue(constant, schema.withoutConstraint("const"), declaredTypeId)
    }

    if (schema["enum"]?.isArray == true && declaredTypeId != null) {
      val enumValue = KotlinIdentifier.requireClassName(value.textValue(), "enum constant")
      return CodeBlock.of("%T.%L", className(declaredTypeId), enumValue)
    }

    return when (schema.typeText()) {
      "object" -> renderObject(value, schema, declaredTypeId)
      "array" -> renderArray(value, schema)
      "string" -> renderString(value.textValue(), schema)
      "integer" -> if (schema["format"]?.textValue() == "int32") {
        CodeBlock.of("%L", value.intValue())
      } else {
        CodeBlock.of("%LL", value.longValue())
      }
      "number" -> CodeBlock.of("%T(%S)", java.math.BigDecimal::class, value.asText())
      "boolean" -> CodeBlock.of("%L", value.booleanValue())
      null -> renderComposedObject(value, schema, declaredTypeId)
      else -> error("Unsupported JSON Schema type '${schema.typeText()}' while rendering a specification value.")
    }
  }

  private fun renderStructuredValue(value: JsonNode, schema: JsonNode): CodeBlock {
    if (value.isNull) {
      return CodeBlock.of("null")
    }

    schema["\$ref"]?.takeIf(JsonNode::isTextual)?.textValue()?.let { reference ->
      val referencedTypeId = reference.localTypeId()
      val referencedSchema = requireNotNull(schemaNodesByTypeId[referencedTypeId]) {
        "Schema references unknown local schema '$referencedTypeId'."
      }
      return renderStructuredValue(value, referencedSchema)
    }

    schema["const"]?.let { constant ->
      val unconstrainedSchema = schema.withoutConstraint("const")
      return if (unconstrainedSchema.typeText() == null && !unconstrainedSchema.has("allOf")) {
        renderJsonLiteral(constant)
      } else {
        renderStructuredValue(constant, unconstrainedSchema)
      }
    }

    return when (schema.typeText()) {
      "object" -> renderStructuredObject(value, schema)
      "array" -> {
        require(value.isArray) { "Expected an array specification error value." }
        val itemSchema = requireNotNull(schema["items"]) { "Array schema must define items." }
        val function = if (schema["uniqueItems"]?.booleanValue() == true) "setOf" else "listOf"
        collectionCall(
          MemberName("kotlin.collections", function),
          value.map { renderStructuredValue(it, itemSchema) }
        )
      }
      "string" -> renderString(value.textValue(), schema)
      "integer" -> if (schema["format"]?.textValue() == "int32") {
        CodeBlock.of("%L", value.intValue())
      } else {
        CodeBlock.of("%LL", value.longValue())
      }
      "number" -> CodeBlock.of("%T(%S)", java.math.BigDecimal::class, value.asText())
      "boolean" -> CodeBlock.of("%L", value.booleanValue())
      null -> {
        require(schema.has("allOf")) { "Only allOf object composition is supported for specification errors." }
        renderStructuredObject(value, schema)
      }
      else -> error("Unsupported JSON Schema type '${schema.typeText()}' while rendering a specification error.")
    }
  }

  private fun renderStructuredObject(value: JsonNode, schema: JsonNode): CodeBlock {
    require(value.isObject) { "Expected an object specification error value." }
    if (schema.isDictionaryOnlyObject()) {
      val keySchema = schema["propertyNames"] ?: objectMapper.createObjectNode().put("type", "string")
      val valueSchema = requireNotNull(schema["additionalProperties"])
      return collectionCall(
        MemberName("kotlin.collections", "mapOf"),
        value.properties().map { (key, entryValue) ->
          CodeBlock.of(
            "%L to %L",
            renderStructuredValue(objectMapper.nodeFactory.textNode(key), keySchema),
            renderStructuredValue(entryValue, valueSchema)
          )
        }.toList()
      )
    }

    val entries = objectProperties(schema).mapNotNull { (propertyName, propertySchema) ->
      val propertyValue = value[propertyName] ?: propertySchema["const"] ?: return@mapNotNull null
      CodeBlock.of("%S to %L", propertyName, renderStructuredValue(propertyValue, propertySchema))
    }
    return collectionCall(MemberName("kotlin.collections", "mapOf"), entries)
  }

  private fun renderJsonLiteral(value: JsonNode): CodeBlock = when {
    value.isNull -> CodeBlock.of("null")
    value.isTextual -> CodeBlock.of("%S", value.textValue())
    value.isIntegralNumber -> CodeBlock.of("%LL", value.longValue())
    value.isFloatingPointNumber -> CodeBlock.of("%T(%S)", java.math.BigDecimal::class, value.asText())
    value.isBoolean -> CodeBlock.of("%L", value.booleanValue())
    value.isArray -> collectionCall(MemberName("kotlin.collections", "listOf"), value.map(::renderJsonLiteral))
    value.isObject -> collectionCall(
      MemberName("kotlin.collections", "mapOf"),
      value.properties().map { (key, child) -> CodeBlock.of("%S to %L", key, renderJsonLiteral(child)) }.toList()
    )
    else -> error("Unsupported JSON constant '$value'.")
  }

  private fun renderObject(value: JsonNode, schema: JsonNode, declaredTypeId: String?): CodeBlock {
    if (schema.isDictionaryOnlyObject()) {
      val keySchema = schema["propertyNames"] ?: objectMapper.createObjectNode().put("type", "string")
      val valueSchema = requireNotNull(schema["additionalProperties"])
      val entries = value.properties().map { (key, entryValue) ->
        CodeBlock.of(
          "%L to %L",
          renderValue(objectMapper.nodeFactory.textNode(key), keySchema),
          renderValue(entryValue, valueSchema)
        )
      }.toList()
      return collectionCall(MemberName("kotlin.collections", "mapOf"), entries)
    }

    require(value.isObject) { "Expected an object value for schema '$declaredTypeId'." }
    val typeId = requireNotNull(declaredTypeId) {
      "Inline object values in specifications are only supported as properties of a named schema."
    }
    val arguments = objectProperties(schema).mapNotNull { (propertyName, propertySchema) ->
      val propertyValue = value[propertyName] ?: propertySchema["const"] ?: return@mapNotNull null
      KotlinIdentifier.requirePropertyName(propertyName)
      CodeBlock.of("%L = %L", propertyName, renderValue(propertyValue, propertySchema))
    }
    return CodeBlock.builder()
      .add("%T(", className(typeId))
      .addSeparated(arguments)
      .add(")")
      .build()
  }

  private fun renderComposedObject(value: JsonNode, schema: JsonNode, declaredTypeId: String?): CodeBlock {
    require(schema.has("allOf")) {
      "Only allOf object composition is supported for specification values of '$declaredTypeId'."
    }
    return renderObject(value, schema, declaredTypeId)
  }

  private fun renderArray(value: JsonNode, schema: JsonNode): CodeBlock {
    require(value.isArray) { "Expected an array specification value." }
    val itemSchema = requireNotNull(schema["items"]) { "Array schema must define items." }
    val values = value.map { renderValue(it, itemSchema) }
    val function = if (schema["uniqueItems"]?.booleanValue() == true) "setOf" else "listOf"
    return collectionCall(MemberName("kotlin.collections", function), values)
  }

  private fun renderString(value: String, schema: JsonNode): CodeBlock = when (schema["format"]?.textValue()) {
    "uuid" -> renderUuid(value)
    "date" -> CodeBlock.of("%T.parse(%S)", java.time.LocalDate::class, value)
    "date-time" -> CodeBlock.of("%T.parse(%S)", java.time.OffsetDateTime::class, value)
    "time" -> CodeBlock.of("%T.parse(%S)", java.time.OffsetTime::class, value)
    "duration" -> CodeBlock.of("%T.parse(%S)", java.time.Duration::class, value)
    "uri", "uri-reference" -> CodeBlock.of("%T.create(%S)", java.net.URI::class, value)
    else -> CodeBlock.of("%S", value)
  }

  private fun renderUuid(value: String): CodeBlock {
    val uuid = runCatching { java.util.UUID.fromString(value) }.getOrNull()
    return if (uuid != null && uuid.toString().equals(value, ignoreCase = true)) {
      CodeBlock.of("%T.fromString(%S)", java.util.UUID::class, value)
    } else {
      CodeBlock.of("%T.nameUUIDFromBytes(%S.toByteArray())", java.util.UUID::class, value)
    }
  }

  private fun objectProperties(schema: JsonNode): LinkedHashMap<String, JsonNode> {
    val properties = linkedMapOf<String, JsonNode>()
    schema["allOf"]?.forEach { part ->
      val resolved = part["\$ref"]?.textValue()?.let { schemaNodesByTypeId[it.localTypeId()] } ?: part
      if (resolved != null) properties.putAll(objectProperties(resolved))
    }
    schema["properties"]?.properties()?.forEach { (name, propertySchema) -> properties[name] = propertySchema }
    return properties
  }

  private fun collectionCall(function: MemberName, values: List<CodeBlock>): CodeBlock = CodeBlock.builder()
    .add("%M(", function)
    .addSeparated(values)
    .add(")")
    .build()

  private fun CodeBlock.Builder.addSeparated(values: List<CodeBlock>): CodeBlock.Builder = apply {
    values.forEachIndexed { index, value ->
      if (index > 0) add(", ")
      add(value)
    }
  }

  private fun JsonNode.typeText(): String? {
    val type = this["type"] ?: return null
    if (type.isTextual) return type.textValue()
    if (type.isArray) return type.firstOrNull { it.textValue() != "null" }?.textValue()
    return null
  }

  private fun JsonNode.isDictionaryOnlyObject(): Boolean {
    if (typeText() != "object" || has("allOf") || has("oneOf") || has("anyOf")) return false
    val properties = this["properties"]
    if (properties != null && (!properties.isObject || !properties.isEmpty)) return false
    return this["additionalProperties"]?.isObject == true
  }

  private fun JsonNode.withoutConstraint(name: String): JsonNode = deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>().apply {
    remove(name)
  }

  private fun String.localTypeId(): String = substringBefore('#').substringAfterLast('/').removeSuffix(".schema.json")
}
