package io.holixon.emn.gradle

import io.holixon.emn.generation.specification.EmnSpecificationTestGenerationRequest
import io.holixon.emn.generation.specification.EmnSpecificationTestGenerationService
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class GenerateEmnSpecificationTestsTask : DefaultTask() {

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
  abstract val generationEnabled: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val fixtureProvider: Property<String>

  @get:Input
  @get:Optional
  abstract val errorVerifier: Property<String>

  @TaskAction
  fun generate() {
    fileSystemOperations.delete { it.delete(outputDirectory) }
    val fixtureProviderClassName = fixtureProvider.orNull?.takeIf(String::isNotBlank)
      ?: throw GradleException(
        "emn.specifications.fixtureProvider is required when specification generation is enabled."
      )
    val result = EmnSpecificationTestGenerationService().generate(
      EmnSpecificationTestGenerationRequest(
        emnFiles = emnFiles.files.sortedBy { it.path },
        outputDirectory = outputDirectory.get().asFile,
        rootPackageName = rootPackageName.get(),
        fixtureProviderClassName = fixtureProviderClassName,
        errorVerifierClassName = errorVerifier.orNull,
      )
    )
    result.generatedFiles.forEach { logger.info("Generated EMN specification test: $it") }
  }
}
