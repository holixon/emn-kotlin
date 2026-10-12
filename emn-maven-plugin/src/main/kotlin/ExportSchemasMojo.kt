package io.holixon.emn.maven

import io.holixon.emn.generation.schema.EmnSchemaExportRequest
import io.holixon.emn.generation.schema.EmnSchemaExportService
import org.apache.maven.model.Resource
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(name = "export-schemas", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true)
class ExportSchemasMojo : AbstractEmnMojo() {

  @Parameter(
    property = "emn.schemaExport.outputDirectory",
    required = true,
    defaultValue = $$"${project.build.directory}/generated-resources/emn-schemas",
  )
  private lateinit var outputDirectory: File

  @Parameter(property = "emn.schemaResourceDirectories")
  private var schemaResourceDirectories: Array<File> = emptyArray()

  @Parameter(property = "emn.schemaExport.sourceRoot", required = true, defaultValue = $$"${project.basedir}")
  private lateinit var sourceRoot: File

  override fun execute() {
    outputDirectory.deleteRecursively()
    val emnFiles = findEmnFiles()
    if (emnFiles.isEmpty()) return

    val result = EmnSchemaExportService().export(EmnSchemaExportRequest(
      emnFiles = emnFiles,
      outputDirectory = outputDirectory,
      sourceRoot = sourceRoot,
      schemaResourceDirectories = schemaResourceDirectories.toList() + resourceDirectory,
    ))
    mojoContext.mavenProject?.addResource(Resource().apply { directory = outputDirectory.absolutePath })
    log.info("Exported ${result.schemaFiles.size} EMN schema(s) and manifest to $outputDirectory.")
    result.schemaFiles.forEach { log.debug("Exported EMN schema: $it") }
  }
}
