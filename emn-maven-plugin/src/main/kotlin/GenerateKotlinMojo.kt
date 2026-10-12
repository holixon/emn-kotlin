package io.holixon.emn.maven

import io.holixon.emn.generation.schema.EmnSchemaGenerationRequest
import io.holixon.emn.generation.schema.EmnSchemaGenerationService
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(name = "generate-kotlin", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresProject = true)
class GenerateKotlinMojo : AbstractEmnMojo() {

  @Parameter(
    property = "emn.kotlin.outputDirectory",
    required = true,
    defaultValue = $$"${project.build.directory}/generated-sources/emn-kotlin",
  )
  private lateinit var outputDirectory: File

  @Parameter(property = "emn.rootPackageName", required = true)
  private lateinit var rootPackageName: String

  @Parameter(property = "emn.kotlin.annotationEnhancers")
  private var annotationEnhancers: Array<String> = emptyArray()

  override fun execute() {
    outputDirectory.deleteRecursively()
    mojoContext.mavenProject?.addCompileSourceRoot(outputDirectory.absolutePath)
    val emnFiles = findEmnFiles()
    if (emnFiles.isEmpty()) return

    val result = EmnSchemaGenerationService().generate(EmnSchemaGenerationRequest(
      emnFiles = emnFiles,
      outputDirectory = outputDirectory,
      rootPackageName = rootPackageName,
      annotationEnhancers = annotationEnhancers.toList(),
    ))
    log.info("Generated ${result.generatedFiles.size} Kotlin file(s) from ${emnFiles.size} EMN file(s).")
    result.generatedFiles.forEach { log.debug("Generated EMN Kotlin: $it") }
  }
}
