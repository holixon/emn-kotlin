package io.holixon.emn.generation.schema

import io.holixon.emn.model.Definitions
import java.io.File

class EmnSchemaKotlinGenerator(
  private val extractor: EmnJsonSchemaExtractor = EmnJsonSchemaExtractor(),
  private val hintValidator: EmnJsonSchemaHintValidator = EmnJsonSchemaHintValidator(),
  private val jsonSchemaGenerator: JsonSchemaKotlinCodeGenerator = JsonSchemaKotlinCodeGenerator(),
  private val annotationEnhancers: List<KotlinSchemaAnnotationEnhancer> = emptyList(),
  private val packageLayout: EmnSchemaPackageLayout = EmnSchemaPackageLayout()
) {

  fun generateToDirectory(
    definitions: Definitions,
    rootPackageName: String,
    outputDirectory: File,
    source: String? = null
  ): List<File> = generateToDirectory(
    schemas = extractor.extract(definitions = definitions, source = source),
    rootPackageName = rootPackageName,
    outputDirectory = outputDirectory
  )

  fun generateToDirectory(schemas: List<EmnSchemaDefinition>, rootPackageName: String, outputDirectory: File): List<File> {
    val duplicateTypeIds = schemas.groupBy { it.typeId }.filterValues { it.size > 1 }.keys
    require(duplicateTypeIds.isEmpty()) {
      "Duplicate schema TypeIds found: ${duplicateTypeIds.sorted().joinToString(", ")}."
    }
    hintValidator.validate(schemas)

    val generatedFiles = jsonSchemaGenerator.generateToDirectory(
      schemas = schemas,
      rootPackageName = rootPackageName,
      outputDirectory = outputDirectory
    )
    val context = KotlinSchemaEnhancementContext(
      schemasByTypeId = schemas.associateBy { it.typeId },
      generatedFilesByTypeId = generatedFiles.associateBy { it.nameWithoutExtension },
      packageNamesByTypeId = schemas.associate { schema ->
        schema.typeId to packageLayout.packageName(rootPackageName, schema.typeKind)
      }
    )
    annotationEnhancers.forEach { enhancer -> enhancer.enhance(context) }
    return generatedFiles
  }
}
