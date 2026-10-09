package io.holixon.emn.generation.schema

import java.io.File

class Axon5KotlinSchemaAnnotationEnhancer(
  private val hintExtractor: EmnJsonSchemaHintExtractor = EmnJsonSchemaHintExtractor()
) : KotlinSchemaAnnotationEnhancer {

  override val id: String = "axon5"

  override fun enhance(context: KotlinSchemaEnhancementContext) {
    context.schemasByTypeId.values.forEach { schema ->
      val generatedFile = context.generatedFilesByTypeId[schema.typeId] ?: return@forEach
      enhance(generatedFile, schema)
    }
  }

  private fun enhance(generatedFile: File, schema: EmnSchemaDefinition) {
    val hints = hintExtractor.extract(schema)
    if (hints.isEmpty()) {
      return
    }

    var content = generatedFile.readText()

    hints.forEach { hint ->
      if (hint.entityId) {
        content = content.annotateProperty(
          propertyName = hint.propertyName,
          annotation = "@TargetEntityId"
        )
      }
      if (hint.tagKey != null) {
        content = content.annotateProperty(
          propertyName = hint.propertyName,
          annotation = """@EventTag(key = "${hint.tagKey}")"""
        )
      }
    }

    if (hints.any { it.entityId }) {
      content = content.withImport("org.axonframework.modelling.annotation.TargetEntityId")
    }
    if (hints.any { it.tagKey != null }) {
      content = content.withImport("org.axonframework.eventsourcing.annotation.EventTag")
    }

    generatedFile.writeText(content)
  }

  private fun String.annotateProperty(propertyName: String, annotation: String): String {
    val propertyRegex = Regex("""(val\s+${Regex.escape(propertyName)}\s*:)""")
    require(propertyRegex.containsMatchIn(this)) {
      "Could not add '$annotation' to generated property '$propertyName'."
    }
    return replaceFirst(propertyRegex, "$annotation\n$1")
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
}
