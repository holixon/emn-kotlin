package io.holixon.emn.generation.schema.gradle

import io.holixon.emn.generation.schema.EmnSchemaExportRequest
import io.holixon.emn.generation.schema.EmnSchemaExportService
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class ExportEmnSchemasTask : DefaultTask() {

  @get:Inject
  abstract val fileSystemOperations: FileSystemOperations

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val emnFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaResourceDirectories: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @get:Input
  abstract val sourceRoot: Property<String>

  @TaskAction
  fun export() {
    fileSystemOperations.delete { it.delete(outputDirectory) }
    if (emnFiles.isEmpty) {
      logger.warn("No EMN files configured for schema export.")
      return
    }
    val result = EmnSchemaExportService().export(EmnSchemaExportRequest(
      emnFiles = emnFiles.files.sortedBy { it.path },
      outputDirectory = outputDirectory.get().asFile,
      sourceRoot = File(sourceRoot.get()),
      schemaResourceDirectories = schemaResourceDirectories.files
    ))
    result.schemaFiles.forEach { logger.info("Exported EMN JSON Schema: $it") }
    logger.info("Generated EMN schema manifest: ${result.manifestFile}")
  }
}
