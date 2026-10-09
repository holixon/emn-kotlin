package io.holixon.emn.generation.schema

import java.io.File

data class KotlinSchemaEnhancementContext(
  val schemasByTypeId: Map<String, EmnSchemaDefinition>,
  val generatedFilesByTypeId: Map<String, File>,
  val packageNamesByTypeId: Map<String, String>
)

interface KotlinSchemaAnnotationEnhancer {
  val id: String

  fun enhance(context: KotlinSchemaEnhancementContext)
}
