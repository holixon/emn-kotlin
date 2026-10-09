package io.holixon.emn.generation.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.model.CommandType
import io.holixon.emn.model.EmbeddedSchema
import io.holixon.emn.model.EventType
import io.holixon.emn.model.FlowNodeType
import io.holixon.emn.model.QueryType
import io.holixon.emn.model.ReferencedSchema
import io.holixon.emn.model.ResourceSchema
import io.holixon.emn.model.Schema
import java.io.File
import java.net.URI

data class EmnSchemaExportRequest(
  val emnFiles: Collection<File>,
  val outputDirectory: File,
  val sourceRoot: File? = null,
  val schemaResourceDirectories: Collection<File> = emptyList()
)

data class EmnSchemaExportResult(
  val manifestFile: File,
  val schemaFiles: List<File>,
  val entries: List<EmnSchemaManifestEntry>
)

data class EmnSchemaManifestEntry(
  val typeId: String,
  val kind: String,
  val schemaFormat: String,
  val file: String,
  val source: EmnSchemaManifestSource,
  val references: List<String>
)

data class EmnSchemaManifestSource(
  val emnFile: String,
  val elementId: String,
  val origin: String
)

class EmnSchemaExportService(
  private val objectMapper: ObjectMapper = ObjectMapper(),
  private val acceptedSchemaFormats: Set<String> = EmnJsonSchemaExtractor.DEFAULT_JSON_SCHEMA_FORMATS
) {

  fun export(request: EmnSchemaExportRequest): EmnSchemaExportResult {
    val emnFiles = request.emnFiles.map { it.canonicalFile }.sortedBy { it.path }
    require(emnFiles.isNotEmpty()) { "At least one EMN file is required." }
    emnFiles.forEach { require(it.isFile) { "EMN file does not exist: $it" } }

    val candidates = emnFiles.flatMap { extractCandidates(it) }
    val duplicates = candidates.groupBy { it.typeId }.filterValues { it.size > 1 }.keys.sorted()
    require(duplicates.isEmpty()) { "Duplicate EMN schema type IDs: ${duplicates.joinToString()}" }

    val knownTypeIds = candidates.map { it.typeId }.toSet()
    val schemaDirectory = File(request.outputDirectory, SCHEMA_DIRECTORY)
    schemaDirectory.mkdirs()

    val exported = candidates.sortedBy { it.typeId }.map { candidate ->
      validateTypeId(candidate.typeId)
      val content = readSchema(candidate, request)
      val schema = parseSchema(candidate, content)
      val references = linkedSetOf<String>()
      normalizeSchema(schema, candidate.typeId, knownTypeIds, references)

      val relativeFile = "$SCHEMA_DIRECTORY/${candidate.typeId}$SCHEMA_FILE_SUFFIX"
      val outputFile = File(request.outputDirectory, relativeFile)
      outputFile.parentFile.mkdirs()
      outputFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema) + "\n")

      val entry = EmnSchemaManifestEntry(
        typeId = candidate.typeId,
        kind = candidate.kind.manifestName,
        schemaFormat = candidate.schema.schemaFormat,
        file = relativeFile,
        source = EmnSchemaManifestSource(
          emnFile = sourcePath(candidate.emnFile, request.sourceRoot),
          elementId = candidate.typeId,
          origin = candidate.origin
        ),
        references = references.sorted()
      )
      outputFile to entry
    }

    val entries = exported.map { it.second }
    val manifest = objectMapper.createObjectNode().apply {
      put("format", MANIFEST_FORMAT)
      put("version", MANIFEST_VERSION)
      set<ArrayNode>("schemas", objectMapper.createArrayNode().apply {
        entries.forEach { entry -> add(manifestEntry(entry)) }
      })
    }
    val manifestFile = File(request.outputDirectory, MANIFEST_FILE)
    manifestFile.parentFile.mkdirs()
    manifestFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n")

    return EmnSchemaExportResult(
      manifestFile = manifestFile,
      schemaFiles = exported.map { it.first },
      entries = entries
    )
  }

  private fun extractCandidates(emnFile: File): List<SchemaCandidate> {
    val definitions = EmnDocumentParser().parseTypeDefinitions(emnFile)
    return definitions.schemaDefinitions.mapNotNull { definition ->
      definition.schema?.takeIf { it.schemaFormat in acceptedSchemaFormats }?.let { schema ->
        SchemaCandidate(definition.id, EmnSchemaTypeKind.SCHEMA_DEFINITION, schema, emnFile)
      }
    } + definitions.nodeTypes.mapNotNull { node ->
      val kind = node.kind() ?: return@mapNotNull null
      node.schema?.takeIf { it.schemaFormat in acceptedSchemaFormats }?.let { schema ->
        SchemaCandidate(node.id, kind, schema, emnFile)
      }
    }
  }

  private fun readSchema(candidate: SchemaCandidate, request: EmnSchemaExportRequest): String =
    when (val schema = candidate.schema) {
      is EmbeddedSchema -> schema.content
      is ReferencedSchema -> objectMapper.createObjectNode().put("\$ref", schema.reference).toString()
      is ResourceSchema -> resolveResource(schema.resource, candidate.emnFile, request).readText()
    }

  private fun resolveResource(resource: String, emnFile: File, request: EmnSchemaExportRequest): File {
    val configuredRoots = request.schemaResourceDirectories.map { it.canonicalFile }
    val allowedRoots = (listOf(emnFile.parentFile.canonicalFile) + configuredRoots).distinct()
    val uri = resource.toUriWithScheme()
    val candidates = when {
      uri != null && uri.scheme != "file" ->
        throw IllegalArgumentException("Only local filesystem schema resources are supported: $resource")
      uri != null -> listOf(File(uri))
      File(resource).isAbsolute -> listOf(File(resource))
      else -> listOf(File(emnFile.parentFile, resource)) + configuredRoots.map { File(it, resource) }
    }.map { it.canonicalFile }.distinct()

    val matches = candidates.filter { file ->
      file.isFile && allowedRoots.any { root -> file.toPath().startsWith(root.toPath()) }
    }
    require(matches.size == 1) {
      when {
        matches.isEmpty() -> "Local schema resource '$resource' was not found below an allowed resource directory."
        else -> "Local schema resource '$resource' is ambiguous: ${matches.joinToString()}"
      }
    }
    return matches.single()
  }

  private fun parseSchema(candidate: SchemaCandidate, content: String): JsonNode =
    try {
      objectMapper.readTree(content)
    } catch (exception: Exception) {
      throw IllegalArgumentException("Invalid JSON Schema for '${candidate.typeId}' in ${candidate.emnFile}.", exception)
    }

  private fun normalizeSchema(
    schema: JsonNode,
    typeId: String,
    knownTypeIds: Set<String>,
    references: MutableSet<String>
  ) {
    if (schema is ObjectNode) {
      if (!schema.has("\$id")) schema.put("\$id", typeId)
      if (!schema.has("title")) schema.put("title", typeId)
    }
    rewriteReferences(schema, knownTypeIds, references)
  }

  private fun rewriteReferences(
    node: JsonNode,
    knownTypeIds: Set<String>,
    references: MutableSet<String>
  ) {
    when (node) {
      is ObjectNode -> {
        node.get("\$ref")?.takeIf { it.isTextual }?.let { referenceNode ->
          val reference = referenceNode.asText()
          if (!reference.startsWith("#")) {
            val (target, fragment) = splitReference(reference)
            require(target in knownTypeIds) {
              "Unresolved or non-local JSON Schema reference '$reference'. " +
                "References must target a schema declared in the exported EMN files."
            }
            references += target
            node.put("\$ref", "./$target$SCHEMA_FILE_SUFFIX$fragment")
          }
        }
        node.properties().map { it.value }.forEach { child ->
          rewriteReferences(child, knownTypeIds, references)
        }
      }
      is ArrayNode -> node.forEach { child -> rewriteReferences(child, knownTypeIds, references) }
    }
  }

  private fun splitReference(reference: String): Pair<String, String> {
    val hashIndex = reference.indexOf('#')
    val path = if (hashIndex >= 0) reference.substring(0, hashIndex) else reference
    val fragment = if (hashIndex >= 0) reference.substring(hashIndex) else ""
    require(!path.contains(URI_SCHEME)) { "Network and external URI references are not supported: $reference" }
    val target = path.removePrefix("./").removeSuffix(SCHEMA_FILE_SUFFIX).substringAfterLast('/')
    return target to fragment
  }

  private fun manifestEntry(entry: EmnSchemaManifestEntry): ObjectNode = objectMapper.createObjectNode().apply {
    put("typeId", entry.typeId)
    put("kind", entry.kind)
    put("schemaFormat", entry.schemaFormat)
    put("file", entry.file)
    set<ObjectNode>("source", objectMapper.createObjectNode().apply {
      put("emnFile", entry.source.emnFile)
      put("elementId", entry.source.elementId)
      put("origin", entry.source.origin)
    })
    set<ArrayNode>("references", objectMapper.createArrayNode().apply {
      entry.references.forEach { add(it) }
    })
  }

  private fun sourcePath(file: File, sourceRoot: File?): String {
    val path = file.canonicalFile.toPath()
    val displayPath = sourceRoot?.canonicalFile?.toPath()?.relativize(path)?.toString() ?: file.name
    return displayPath.replace(File.separatorChar, '/')
  }

  private fun validateTypeId(typeId: String) {
    require(TYPE_ID.matches(typeId)) {
      "Schema type ID '$typeId' cannot be used as a portable schema filename."
    }
  }

  private fun FlowNodeType.kind(): EmnSchemaTypeKind? = when (this) {
    is CommandType -> EmnSchemaTypeKind.COMMAND
    is EventType -> EmnSchemaTypeKind.EVENT
    is QueryType -> EmnSchemaTypeKind.QUERY
    else -> null
  }

  private val EmnSchemaTypeKind.manifestName: String
    get() = when (this) {
      EmnSchemaTypeKind.COMMAND -> "command"
      EmnSchemaTypeKind.EVENT -> "event"
      EmnSchemaTypeKind.QUERY -> "query"
      EmnSchemaTypeKind.SCHEMA_DEFINITION -> "common"
    }

  private val SchemaCandidate.origin: String
    get() = when (schema) {
      is EmbeddedSchema -> "embedded"
      is ResourceSchema -> "resource"
      is ReferencedSchema -> "reference"
    }

  private fun String.toUriWithScheme(): URI? =
    runCatching { URI(this) }.getOrNull()?.takeIf { it.scheme != null }

  private data class SchemaCandidate(
    val typeId: String,
    val kind: EmnSchemaTypeKind,
    val schema: Schema,
    val emnFile: File
  )

  companion object {
    const val MANIFEST_FORMAT = "io.holixon.emn.schema-export"
    const val MANIFEST_VERSION = 1
    const val MANIFEST_FILE = "manifest.json"
    const val SCHEMA_DIRECTORY = "schemas"
    const val SCHEMA_FILE_SUFFIX = ".schema.json"

    private val TYPE_ID = Regex("[A-Za-z_][A-Za-z0-9_.-]*")
    private val URI_SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
  }
}
