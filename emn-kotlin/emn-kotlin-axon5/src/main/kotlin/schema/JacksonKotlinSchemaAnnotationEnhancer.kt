package io.holixon.emn.generation.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class JacksonKotlinSchemaAnnotationEnhancer(
  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
) : KotlinSchemaAnnotationEnhancer {

  override val id: String = "jackson"

  override fun enhance(context: KotlinSchemaEnhancementContext) {
    val schemaNodes = context.schemasByTypeId.mapValues { (_, schema) ->
      objectMapper.readTree(schema.schemaContent)
    }

    context.schemasByTypeId.forEach { (typeId, _) ->
      val schemaNode = schemaNodes.getValue(typeId)
      val discriminator = schemaNode["discriminator"] ?: return@forEach
      val propertyName = discriminator["propertyName"]?.takeIf(JsonNode::isTextual)?.textValue()
      require(!propertyName.isNullOrBlank()) {
        "Polymorphic schema '$typeId' must define a textual discriminator.propertyName."
      }

      val subtypes = schemaNode.discriminatorSubtypes(typeId, propertyName, schemaNodes)

      val duplicateValues = subtypes.groupBy { it.second }.filterValues { it.size > 1 }.keys
      require(duplicateValues.isEmpty()) {
        "Polymorphic schema '$typeId' has duplicate discriminator values: ${duplicateValues.sorted().joinToString(", ")}."
      }

      val generatedFile = requireNotNull(context.generatedFilesByTypeId[typeId]) {
        "No generated Kotlin file found for polymorphic schema '$typeId'."
      }
      var content = generatedFile.readText()
      content = content.annotateDeclaration(
        typeId = typeId,
        annotation = jacksonAnnotations(propertyName, subtypes)
      )
      content = content.withImport("com.fasterxml.jackson.annotation.JsonSubTypes")
      content = content.withImport("com.fasterxml.jackson.annotation.JsonTypeInfo")
      val ownerPackage = context.packageNamesByTypeId.getValue(typeId)
      subtypes.map { it.first }.forEach { subtypeId ->
        val subtypePackage = context.packageNamesByTypeId.getValue(subtypeId)
        if (subtypePackage != ownerPackage) {
          content = content.withImport("$subtypePackage.$subtypeId")
        }
      }
      generatedFile.writeText(content)
    }
  }

  private fun JsonNode.discriminatorSubtypes(
    typeId: String,
    propertyName: String,
    schemas: Map<String, JsonNode>
  ): List<Pair<String, String>> {
    val oneOf = this["oneOf"]
    if (oneOf?.isArray == true && !oneOf.isEmpty) {
      return oneOf.map { alternative ->
        val reference = alternative["\$ref"]?.takeIf(JsonNode::isTextual)?.textValue()
        require(reference != null) {
          "Polymorphic schema '$typeId' only supports oneOf alternatives using local \$ref values."
        }
        val subtypeId = reference.localTypeId()
        subtypeId to requireDiscriminatorValue(typeId, subtypeId, propertyName, schemas)
      }
    }

    val mapping = this["discriminator"]?.get("mapping")
    require(mapping?.isObject == true && !mapping.isEmpty) {
      "Polymorphic schema '$typeId' must define a non-empty oneOf array or discriminator.mapping object."
    }
    return mapping.properties().asSequence().map { (discriminatorValue, target) ->
      require(target.isTextual && target.textValue().isNotBlank()) {
        "Polymorphic schema '$typeId' discriminator mapping '$discriminatorValue' must reference a subtype by name."
      }
      val subtypeId = target.textValue().localTypeId()
      val actualValue = requireDiscriminatorValue(typeId, subtypeId, propertyName, schemas)
      require(actualValue == discriminatorValue) {
        "Subtype '$subtypeId' of polymorphic schema '$typeId' defines discriminator value '$actualValue', but mapping uses '$discriminatorValue'."
      }
      subtypeId to discriminatorValue
    }.toList()
  }

  private fun requireDiscriminatorValue(
    typeId: String,
    subtypeId: String,
    propertyName: String,
    schemas: Map<String, JsonNode>
  ): String {
    KotlinIdentifier.requireClassName(subtypeId, "Jackson subtype from schema '$typeId'")
    require(subtypeId in schemas) {
      "Polymorphic schema '$typeId' references unknown subtype '$subtypeId'."
    }
    return requireNotNull(
      schemas.getValue(subtypeId)
        .findDiscriminatorConstants(propertyName, schemas, setOf(subtypeId))
        .distinct()
        .singleOrNull()
    ) {
      "Subtype '$subtypeId' of polymorphic schema '$typeId' must define exactly one textual const for discriminator property '$propertyName'."
    }
  }

  private fun JsonNode.findDiscriminatorConstants(
    propertyName: String,
    schemas: Map<String, JsonNode>,
    visitedTypeIds: Set<String>
  ): List<String> {
    val result = mutableListOf<String>()
    this["properties"]?.get(propertyName)?.get("const")
      ?.takeIf(JsonNode::isTextual)
      ?.textValue()
      ?.let(result::add)

    this["allOf"]?.takeIf(JsonNode::isArray)?.forEach { part ->
      val reference = part["\$ref"]?.takeIf(JsonNode::isTextual)?.textValue()
      if (reference == null) {
        result.addAll(part.findDiscriminatorConstants(propertyName, schemas, visitedTypeIds))
      } else {
        val referencedTypeId = reference.localTypeId()
        if (referencedTypeId !in visitedTypeIds) {
          schemas[referencedTypeId]?.let { referencedSchema ->
            result.addAll(
              referencedSchema.findDiscriminatorConstants(
                propertyName,
                schemas,
                visitedTypeIds + referencedTypeId
              )
            )
          }
        }
      }
    }
    return result
  }

  private fun jacksonAnnotations(propertyName: String, subtypes: List<Pair<String, String>>): String = buildString {
    appendLine("@JsonTypeInfo(")
    appendLine("  use = JsonTypeInfo.Id.NAME,")
    appendLine("  include = JsonTypeInfo.As.EXISTING_PROPERTY,")
    appendLine("  property = ${objectMapper.writeValueAsString(propertyName)},")
    appendLine("  visible = true,")
    appendLine(")")
    appendLine("@JsonSubTypes(")
    subtypes.forEach { (subtypeId, discriminatorValue) ->
      appendLine(
        "  JsonSubTypes.Type(value = $subtypeId::class, name = ${objectMapper.writeValueAsString(discriminatorValue)}),"
      )
    }
    append(")")
  }

  private fun String.annotateDeclaration(typeId: String, annotation: String): String {
    val declarationRegex = Regex("""(?:open\s+)?(?:class|interface)\s+${Regex.escape(typeId)}\b""")
    val declaration = declarationRegex.find(this)
    require(declaration != null) {
      "Could not add Jackson polymorphism annotations to generated declaration '$typeId'."
    }
    return replaceRange(declaration.range, "$annotation\n${declaration.value}")
  }

  private fun String.withImport(importName: String): String {
    if (contains("import $importName")) {
      return this
    }

    val lines = lines().toMutableList()
    val packageIndex = lines.indexOfFirst { it.startsWith("package ") }
    require(packageIndex >= 0) { "Cannot add import '$importName' because no package declaration was found." }

    val importIndexes = lines.withIndex().filter { it.value.startsWith("import ") }.map { it.index }
    if (importIndexes.isEmpty()) {
      lines.add(packageIndex + 1, "")
      lines.add(packageIndex + 2, "import $importName")
      return lines.joinToString("\n")
    }

    val imports = importIndexes.map { lines[it] }.toMutableSet()
    imports.add("import $importName")
    val firstImportIndex = importIndexes.first()
    val lastImportIndex = importIndexes.last()
    repeat(lastImportIndex - firstImportIndex + 1) {
      lines.removeAt(firstImportIndex)
    }
    lines.addAll(firstImportIndex, imports.sorted())
    return lines.joinToString("\n")
  }

  private fun String.localTypeId(): String =
    substringAfterLast('#').substringAfterLast('/').removeSuffix(".schema.json")
}
