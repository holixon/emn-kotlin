package io.holixon.emn.maven

import io.holixon.emn.generation.specification.EmnSpecificationTestGenerationRequest
import io.holixon.emn.generation.specification.EmnSpecificationTestGenerationService
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(name = "generate-specification-tests", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresProject = true)
class GenerateSpecificationTestsMojo : AbstractEmnMojo() {

  @Parameter(
    property = "emn.specifications.outputDirectory",
    required = true,
    defaultValue = $$"${project.build.directory}/generated-test-sources/emn-specification-tests",
  )
  private lateinit var outputDirectory: File

  @Parameter(property = "emn.rootPackageName", required = true)
  private lateinit var rootPackageName: String

  @Parameter(property = "emn.specifications.fixtureProvider", required = true)
  private lateinit var fixtureProvider: String

  @Parameter(property = "emn.specifications.errorVerifier")
  private var errorVerifier: String? = null

  override fun execute() {
    outputDirectory.deleteRecursively()
    mojoContext.mavenProject?.addTestCompileSourceRoot(outputDirectory.absolutePath)
    val emnFiles = findEmnFiles()
    if (emnFiles.isEmpty()) return

    val result = EmnSpecificationTestGenerationService().generate(
      EmnSpecificationTestGenerationRequest(
        emnFiles = emnFiles,
        outputDirectory = outputDirectory,
        rootPackageName = rootPackageName,
        fixtureProviderClassName = fixtureProvider,
        errorVerifierClassName = errorVerifier,
      )
    )
    log.info("Generated ${result.generatedFiles.size} specification test file(s) from ${emnFiles.size} EMN file(s).")
    result.generatedFiles.forEach { log.debug("Generated EMN specification test: $it") }
  }
}
