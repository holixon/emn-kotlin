package io.holixon.emn.generation.gradle

import io.holixon.emn.generation.schema.EmnSchemaGenerationRequest
import io.holixon.emn.generation.schema.EmnSchemaGenerationService
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class GenerateEmnSchemaKotlinTask : DefaultTask() {

  @get:Inject
  abstract val fileSystemOperations: FileSystemOperations

  @get:InputFiles
  @get:SkipWhenEmpty
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val emnFiles: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @get:Input
  abstract val rootPackageName: Property<String>

  @get:Input
  abstract val annotationEnhancers: ListProperty<String>

  @TaskAction
  fun generate() {
    fileSystemOperations.delete { it.delete(outputDirectory) }
    val result = EmnSchemaGenerationService().generate(EmnSchemaGenerationRequest(
      emnFiles = emnFiles.files.sortedBy { it.path },
      outputDirectory = outputDirectory.get().asFile,
      rootPackageName = rootPackageName.get(),
      annotationEnhancers = annotationEnhancers.get()
    ))
    result.generatedFiles.forEach { logger.info("Generated JSON Schema Kotlin: $it") }
  }
}
