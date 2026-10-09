package io.holixon.emn.generation.schema

import io.holixon.emn.EmnDocumentParser
import java.io.File

data class EmnSchemaGenerationRequest(
  val emnFiles: List<File>,
  val outputDirectory: File,
  val rootPackageName: String,
  val annotationEnhancers: List<String> = emptyList()
)

data class EmnSchemaGenerationResult(
  val generatedFiles: List<File>
)

/** Build-tool-neutral entry point for generating Kotlin sources from EMN JSON Schemas. */
class EmnSchemaGenerationService(
  private val parser: EmnDocumentParser = EmnDocumentParser(),
  private val extractor: EmnJsonSchemaExtractor = EmnJsonSchemaExtractor()
) {

  fun generate(request: EmnSchemaGenerationRequest): EmnSchemaGenerationResult {
    require(request.rootPackageName.isNotBlank()) { "rootPackageName must not be blank." }
    request.emnFiles.forEach { require(it.isFile) { "EMN file does not exist: $it" } }
    request.outputDirectory.mkdirs()

    val schemas = request.emnFiles.flatMap { emnFile ->
      extractor.extract(
        definitions = parser.parseTypeDefinitions(emnFile),
        source = emnFile.path
      )
    }
    val generator = EmnSchemaKotlinGenerator(annotationEnhancers = annotationEnhancers(request.annotationEnhancers))
    val generatedFiles = generator.generateToDirectory(
      schemas = schemas,
      rootPackageName = request.rootPackageName,
      outputDirectory = request.outputDirectory
    )

    return EmnSchemaGenerationResult(generatedFiles = generatedFiles)
  }

  private fun annotationEnhancers(names: List<String>): List<KotlinSchemaAnnotationEnhancer> =
    names
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .map { name ->
        when (name.lowercase()) {
          "axon5" -> Axon5KotlinSchemaAnnotationEnhancer()
          "jackson" -> JacksonKotlinSchemaAnnotationEnhancer()
          else -> error("Unknown JSON Schema Kotlin annotation enhancer '$name'. Supported values: axon5, jackson.")
        }
      }
}
