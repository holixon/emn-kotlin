package io.holixon.emn.generation.maven

import com.facebook.ktfmt.format.Formatter
import io.holixon.emn.generation.specification.EmnSpecificationTestGenerationRequest
import io.holixon.emn.generation.specification.EmnSpecificationTestGenerationService
import io.toolisticon.maven.fn.FileExt.createIfNotExists
import io.toolisticon.maven.mojo.AbstractContextAwareMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(
  name = GenerateSpecificationTestsMojo.GOAL,
  defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
  requiresProject = true
)
class GenerateSpecificationTestsMojo : AbstractContextAwareMojo() {

  companion object {
    const val GOAL = "generate-specification-tests"
    const val DEFAULT_OUTPUT_DIRECTORY = "${EmnKotlinAxon5MavenPlugin.BUILD_GENERATED_TEST_SOURCES}/emn-specification-tests"
  }

  @Parameter(
    property = "resourceDirectory",
    required = true,
    defaultValue = EmnKotlinAxon5MavenPlugin.DEFAULT_RESOURCES_DIRECTORY
  )
  private lateinit var resourceDirectory: File

  @Parameter(property = "testOutputDirectory", required = true, defaultValue = DEFAULT_OUTPUT_DIRECTORY)
  private lateinit var testOutputDirectory: File

  @Parameter(property = "includes")
  private var includes: Array<String> = EmnKotlinAxon5MavenPlugin.DEFAULT_INCLUDES

  @Parameter(property = "rootPackageName", required = true)
  private lateinit var rootPackageName: String

  @Parameter(property = "fixtureProvider", required = true)
  private lateinit var fixtureProvider: String

  @Parameter(property = "errorVerifier")
  private var errorVerifier: String? = null

  override fun execute() {
    testOutputDirectory.createIfNotExists()
    mojoContext.mavenProject?.addTestCompileSourceRoot(testOutputDirectory.absolutePath)
    if (!resourceDirectory.exists()) {
      log.warn("Skip non existing resource directory $resourceDirectory.")
      return
    }

    val emnFiles = EmnKotlinAxon5MavenPlugin.findIncludedFiles(
      absPath = resourceDirectory.absolutePath,
      includes = includes
    )
      .map { File(resourceDirectory, it) }
    log.info("Found ${emnFiles.size} EMN file(s) for specification test generation in $resourceDirectory.")
    val result = EmnSpecificationTestGenerationService().generate(
      EmnSpecificationTestGenerationRequest(
        emnFiles = emnFiles,
        outputDirectory = testOutputDirectory,
        rootPackageName = rootPackageName,
        fixtureProviderClassName = fixtureProvider,
        errorVerifierClassName = errorVerifier
      )
    )
    result.generatedFiles.forEach { file ->
      file.writeText(Formatter.format(EmnKotlinAxon5MavenPlugin.KTFMT_FORMAT, file.readText()))
      log.info("Generating EMN specification test: $file")
    }
  }
}
