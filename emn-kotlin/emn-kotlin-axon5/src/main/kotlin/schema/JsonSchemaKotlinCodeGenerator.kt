package io.holixon.emn.generation.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.pwall.json.schema.codegen.CodeGenerator
import net.pwall.json.schema.codegen.TargetLanguage
import java.io.File
import java.nio.file.Files

class JsonSchemaKotlinCodeGenerator(
  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule(),
  private val packageLayout: EmnSchemaPackageLayout = EmnSchemaPackageLayout()
) {

  fun generateToDirectory(schemas: List<EmnSchemaDefinition>, rootPackageName: String, outputDirectory: File): List<File> {
    val rootPackage = KotlinIdentifier.requirePackageName(rootPackageName)
    outputDirectory.mkdirs()

    if (schemas.isEmpty()) {
      return emptyList()
    }

    val generationSchemas = expandInlineRootArraySchemas(schemas)

    val schemaDirectory = Files.createTempDirectory("emn-json-schema-kotlin-").toFile()
    try {
      val localSchemas = generationSchemas.associate { it.typeId to objectMapper.readTree(it.schemaContent) }
      val packagesByTypeId = generationSchemas.associate { schema ->
        schema.typeId to packageLayout.packageName(rootPackage, schema.typeKind)
      }
      val schemaFiles = generationSchemas.associate { schema ->
        val className = KotlinIdentifier.requireClassName(schema.typeId, "class name from TypeId")
        val schemaFile = File(schemaDirectory, "$className.schema.json")
        schemaFile.writeText(schema.normalizedSchemaContent(localSchemas))
        schema.typeId to schemaFile
      }
      val aliasSchemas = localSchemas.filterValues { it.isTypeAliasSchema() }

      CodeGenerator(
        targetLanguage = TargetLanguage.KOTLIN,
        basePackageName = rootPackage,
        baseDirectoryName = outputDirectory.absolutePath,
        derivePackageFromStructure = true,
        generatorComment = null
      ).apply {
        examplesValidationOption = CodeGenerator.ValidationOption.NONE
        defaultValidationOption = CodeGenerator.ValidationOption.NONE
        aliasSchemas.keys.forEach { typeId ->
          val className = KotlinIdentifier.requireClassName(typeId, "class name from TypeId")
          val schemaFile = schemaFiles.getValue(typeId)
          val packageName = packagesByTypeId.getValue(typeId)
          addCustomClassByURI(schemaFile.toURI(), className, packageName)
          addCustomClassByURI(schemaFile.parentFile.toURI().resolve(typeId), className, packageName)
        }
        schemaParser.preLoad(schemaDirectory)
        generationSchemas.filter { it.typeId !in aliasSchemas }.forEach { schema ->
          addTarget(
            schemaFiles.getValue(schema.typeId),
            packageLayout.packageSuffix(schema.typeKind)
          )
        }
        if (numTargets > 0) {
          generateAllTargets()
        }
      }

      aliasSchemas.forEach { (typeId, schemaNode) ->
        val className = KotlinIdentifier.requireClassName(typeId, "class name from TypeId")
        val packageName = packagesByTypeId.getValue(typeId)
        val generatedFile = File(outputDirectory, packageName.replace('.', File.separatorChar))
          .resolve("$className.kt")
        generatedFile.parentFile.mkdirs()
        generatedFile.writeText(
          typeAlias(
            packageName = packageName,
            typeId = className,
            targetType = resolveKotlinType(
              schemaNode = schemaNode,
              localSchemas = localSchemas,
              packagesByTypeId = packagesByTypeId,
              currentPackageName = packageName
            )
          )
        )
      }
    } finally {
      schemaDirectory.deleteRecursively()
    }

    return generationSchemas.mapNotNull { schema ->
      val className = KotlinIdentifier.requireClassName(schema.typeId, "class name from TypeId")
      val packageName = packageLayout.packageName(rootPackage, schema.typeKind)
      File(outputDirectory, packageName.replace('.', File.separatorChar))
        .resolve("$className.kt")
        .takeIf(File::exists)
    }
  }

  private fun expandInlineRootArraySchemas(schemas: List<EmnSchemaDefinition>): List<EmnSchemaDefinition> {
    val existingTypeIds = schemas.mapTo(mutableSetOf()) { it.typeId }
    return schemas.flatMap { schema ->
      val schemaNode = objectMapper.readTree(schema.schemaContent)
      val items = schemaNode["items"]
      if (schemaNode.typeText() != "array" || items?.typeText() != "object") {
        return@flatMap listOf(schema)
      }

      val itemTypeId = "${schema.typeId}Item"
      require(existingTypeIds.add(itemTypeId)) {
        "Schema '${schema.typeId}' uses inline object array items, but generated item type '$itemTypeId' conflicts with another schema."
      }
      val aliasSchema = schemaNode.deepCopy<ObjectNode>().apply {
        set<ObjectNode>("items", objectMapper.createObjectNode().put("\$ref", itemTypeId))
      }
      listOf(
        schema.copy(
          typeId = itemTypeId,
          schemaContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(items)
        ),
        schema.copy(schemaContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(aliasSchema))
      )
    }
  }

  private fun EmnSchemaDefinition.normalizedSchemaContent(localSchemas: Map<String, JsonNode>): String {
    val schemaNode = requireNotNull(localSchemas[typeId])
    val normalized = when (schemaNode.typeText()) {
      "object" -> schemaNode
      "string", "integer", "number", "boolean" -> schemaNode
      "array" -> {
        val items = schemaNode["items"]
        when {
          items?.get("\$ref")?.isTextual == true -> schemaNode
          else -> throw IllegalArgumentException(
            "Schema '$typeId' is a top-level array. Only arrays whose items use a named local \$ref are supported."
          )
        }
      }
      null -> {
        require(schemaNode.has("allOf") || schemaNode.has("oneOf") || schemaNode.has("anyOf")) {
          "Schema '$typeId' has no type and is not composed with allOf, oneOf, or anyOf."
        }
        schemaNode
      }
      else -> throw IllegalArgumentException(
        "Schema '$typeId' must be an object, string enum, composed schema, or a top-level array of objects, but was type='${schemaNode.typeText()}'."
      )
    }

    if (normalized is ObjectNode) {
      normalized.put("\$id", typeId)
      normalized.put("title", typeId)
      normalized.rewriteLocalSchemaReferences(localSchemas.keys)
    }

    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalized)
  }

  private fun JsonNode.rewriteLocalSchemaReferences(localSchemaIds: Set<String>) {
    when (this) {
      is ObjectNode -> {
        val refNode = this["\$ref"]
        if (refNode?.isTextual == true && refNode.textValue() in localSchemaIds) {
          put("\$ref", "${refNode.textValue()}.schema.json")
        }
        properties().forEach { (_, child) -> child.rewriteLocalSchemaReferences(localSchemaIds) }
      }
      is ArrayNode -> forEach { child -> child.rewriteLocalSchemaReferences(localSchemaIds) }
    }
  }

  private fun JsonNode.typeText(): String? {
    val type = this["type"] ?: return null
    if (type.isTextual) {
      return type.textValue()
    }
    if (type.isArray) {
      val nonNullTypes = type.mapNotNull { it.textValue() }.filterNot { it == "null" }
      require(nonNullTypes.size == 1) { "Only nullable single-type arrays are supported for JSON Schema 'type'." }
      return nonNullTypes.single()
    }
    return null
  }

  private fun JsonNode.isDictionaryOnlyObject(): Boolean {
    if (typeText() != "object" || listOf("allOf", "oneOf", "anyOf").any(::has)) {
      return false
    }
    val properties = this["properties"]
    if (properties != null && (!properties.isObject || !properties.isEmpty)) {
      return false
    }
    val additionalProperties = this["additionalProperties"]
    return additionalProperties?.isObject == true && !additionalProperties.isEmpty
  }

  private fun JsonNode.isScalarTypeAlias(): Boolean =
    typeText() in setOf("string", "integer", "number", "boolean") && !has("enum")

  private fun JsonNode.isRootArrayTypeAlias(): Boolean =
    typeText() == "array" && this["items"]?.get("\$ref")?.isTextual == true

  private fun JsonNode.isTypeAliasSchema(): Boolean =
    isDictionaryOnlyObject() || isScalarTypeAlias() || isRootArrayTypeAlias()

  private fun typeAlias(
    packageName: String,
    typeId: String,
    targetType: KotlinType
  ): String {
    return buildString {
      appendLine("package $packageName")
      if (targetType.imports.isNotEmpty()) {
        appendLine()
        targetType.imports.sorted().forEach { appendLine("import $it") }
      }
      appendLine()
      appendLine("typealias $typeId = ${targetType.name}")
    }
  }

  private fun resolveKotlinType(
    schemaNode: JsonNode,
    localSchemas: Map<String, JsonNode>,
    packagesByTypeId: Map<String, String>,
    currentPackageName: String,
    visitedReferences: Set<String> = emptySet()
  ): KotlinType {
    schemaNode["\$ref"]?.takeIf { it.isTextual }?.textValue()?.let { reference ->
      val referencedTypeId = reference.localTypeId()
      require(referencedTypeId !in visitedReferences) {
        "Circular JSON Schema reference while resolving Kotlin type: ${(visitedReferences + referencedTypeId).joinToString(" -> ")}."
      }
      val referencedSchema = requireNotNull(localSchemas[referencedTypeId]) {
        "Schema references unknown local schema '$referencedTypeId'."
      }
      if (referencedSchema.isNamedKotlinType()) {
        KotlinIdentifier.requireClassName(referencedTypeId, "referenced type")
        val referencedPackage = packagesByTypeId.getValue(referencedTypeId)
        val imports = if (referencedPackage == currentPackageName) {
          emptySet()
        } else {
          setOf("$referencedPackage.$referencedTypeId")
        }
        return KotlinType(referencedTypeId, imports)
      }
      return resolveKotlinType(
        referencedSchema,
        localSchemas,
        packagesByTypeId,
        currentPackageName,
        visitedReferences + referencedTypeId
      )
    }

    val nullable = schemaNode.isNullable()
    val type = when (schemaNode.typeText()) {
      "string" -> when (schemaNode["format"]?.textValue()) {
        "uuid" -> KotlinType("UUID", setOf("java.util.UUID"))
        "date" -> KotlinType("LocalDate", setOf("java.time.LocalDate"))
        "date-time" -> KotlinType("OffsetDateTime", setOf("java.time.OffsetDateTime"))
        "time" -> KotlinType("OffsetTime", setOf("java.time.OffsetTime"))
        "duration" -> KotlinType("Duration", setOf("java.time.Duration"))
        "uri", "uri-reference" -> KotlinType("URI", setOf("java.net.URI"))
        else -> KotlinType("String")
      }
      "integer" -> if (schemaNode["format"]?.textValue() == "int32") KotlinType("Int") else KotlinType("Long")
      "number" -> KotlinType("BigDecimal", setOf("java.math.BigDecimal"))
      "boolean" -> KotlinType("Boolean")
      "array" -> {
        val itemType = resolveKotlinType(
          requireNotNull(schemaNode["items"]) { "Dictionary array value schema must define items." },
          localSchemas,
          packagesByTypeId,
          currentPackageName,
          visitedReferences
        )
        val collectionType = if (schemaNode["uniqueItems"]?.booleanValue() == true) "Set" else "List"
        KotlinType("$collectionType<${itemType.name}>", itemType.imports)
      }
      "object" -> if (schemaNode.isDictionaryOnlyObject()) {
        val keyType = schemaNode["propertyNames"]
          ?.let { resolveKotlinType(it, localSchemas, packagesByTypeId, currentPackageName, visitedReferences) }
          ?: KotlinType("String")
        val valueType = resolveKotlinType(
          requireNotNull(schemaNode["additionalProperties"]),
          localSchemas,
          packagesByTypeId,
          currentPackageName,
          visitedReferences
        )
        KotlinType("Map<${keyType.name}, ${valueType.name}>", keyType.imports + valueType.imports)
      } else {
        KotlinType("Map<String, Any?>")
      }
      else -> KotlinType("Any")
    }
    return if (nullable) type.copy(name = "${type.name}?") else type
  }

  private fun JsonNode.isNamedKotlinType(): Boolean =
    isTypeAliasSchema() ||
      typeText() == "object" ||
      has("allOf") ||
      has("oneOf") ||
      has("anyOf") ||
      (typeText() == "string" && this["enum"]?.isArray == true)

  private fun JsonNode.isNullable(): Boolean =
    this["type"]?.takeIf { it.isArray }?.any { it.isTextual && it.textValue() == "null" } == true

  private fun String.localTypeId(): String =
    substringBefore('#').substringAfterLast('/').removeSuffix(".schema.json")

  private data class KotlinType(
    val name: String,
    val imports: Set<String> = emptySet()
  )
}
