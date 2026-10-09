package io.holixon.emn.generation.schema.maven

import io.holixon.emn.generation.schema.EmnSchemaExportRequest
import io.holixon.emn.generation.schema.EmnSchemaExportService
import io.toolisticon.maven.mojo.AbstractContextAwareMojo
import org.apache.maven.model.Resource
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(
  name = ExportSchemasMojo.GOAL,
  defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
  requiresProject = true
)
class ExportSchemasMojo : AbstractContextAwareMojo() {

  companion object {
    const val GOAL = "export-schemas"
    const val DEFAULT_OUTPUT_DIRECTORY = $$"${project.build.directory}/generated-resources/emn-schemas"
  }

  @Parameter(
    property = "resourceDirectory",
    required = true,
    defaultValue = EmnSchemaExporterMavenPlugin.DEFAULT_RESOURCES_DIRECTORY
  )
  private lateinit var resourceDirectory: File

  @Parameter(
    property = "outputDirectory",
    required = true,
    defaultValue = DEFAULT_OUTPUT_DIRECTORY
  )
  private lateinit var outputDirectory: File

  @Parameter(property = "includes")
  private var includes: Array<String> = EmnSchemaExporterMavenPlugin.DEFAULT_INCLUDES

  @Parameter(property = "schemaResourceDirectories")
  private var schemaResourceDirectories: Array<File> = emptyArray()

  @Parameter(
    property = "sourceRoot",
    required = true,
    defaultValue = $$"${project.basedir}"
  )
  private lateinit var sourceRoot: File

  override fun execute() {
    outputDirectory.deleteRecursively()
    if (!resourceDirectory.exists()) {
      log.warn("Skip non existing resource directory $resourceDirectory.")
      return
    }

    val emnFileNames = EmnSchemaExporterMavenPlugin.findIncludedFiles(
      absPath = resourceDirectory.absolutePath,
      includes = includes
    )
    val emnFiles = emnFileNames.map { File(resourceDirectory, it) }
    if (emnFiles.isEmpty()) {
      log.warn("No EMN files found in $resourceDirectory.")
      return
    }

    val result = EmnSchemaExportService().export(EmnSchemaExportRequest(
      emnFiles = emnFiles,
      outputDirectory = outputDirectory,
      sourceRoot = sourceRoot,
      schemaResourceDirectories = schemaResourceDirectories.toList() + resourceDirectory
    ))

    mojoContext.mavenProject?.addResource(Resource().apply {
      directory = outputDirectory.absolutePath
    })
    log.info("Exported ${result.schemaFiles.size} JSON Schemas to $outputDirectory.")
    log.info("Generated schema manifest: ${result.manifestFile}")
  }
}
