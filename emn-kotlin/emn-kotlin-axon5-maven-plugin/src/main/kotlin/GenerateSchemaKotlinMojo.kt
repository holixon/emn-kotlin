package io.holixon.emn.generation.maven

import com.facebook.ktfmt.format.Formatter
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.schema.EmnSchemaGenerationRequest
import io.holixon.emn.generation.schema.EmnSchemaGenerationService
import io.toolisticon.maven.fn.FileExt.createIfNotExists
import io.toolisticon.maven.mojo.AbstractContextAwareMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@OptIn(ExperimentalKotlinPoetApi::class)
@Mojo(
  name = GenerateSchemaKotlinMojo.GOAL,
  defaultPhase = LifecyclePhase.GENERATE_SOURCES,
  requiresProject = true
)
class GenerateSchemaKotlinMojo : AbstractContextAwareMojo() {

  companion object {
    const val GOAL = "generate-schema-kotlin"
    const val DEFAULT_GENERATED_SCHEMA_SOURCES = "${EmnKotlinAxon5MavenPlugin.BUILD_GENERATED_SOURCES}/emn-schema-kotlin"
  }

  @Parameter(
    property = "resourceDirectory",
    required = true,
    defaultValue = EmnKotlinAxon5MavenPlugin.DEFAULT_RESOURCES_DIRECTORY
  )
  private lateinit var resourceDirectory: File

  @Parameter(
    property = "outputDirectory",
    required = true,
    defaultValue = DEFAULT_GENERATED_SCHEMA_SOURCES
  )
  private lateinit var outputDirectory: File

  @Parameter(
    property = "includes",
  )
  private var includes: Array<String> = EmnKotlinAxon5MavenPlugin.DEFAULT_INCLUDES

  @Parameter(
    property = "rootPackageName",
    required = true
  )
  private lateinit var rootPackageName: String

  @Parameter(
    property = "annotationEnhancers"
  )
  private var annotationEnhancers: Array<String> = emptyArray()

  override fun execute() {
    outputDirectory.createIfNotExists()
    mojoContext.mavenProject?.addCompileSourceRoot(outputDirectory.absolutePath)

    if (!resourceDirectory.exists()) {
      log.warn("Skip non existing resource directory $resourceDirectory.")
      return
    }

    val emnFileNames = EmnKotlinAxon5MavenPlugin.findIncludedFiles(
      absPath = resourceDirectory.absolutePath,
      includes = includes,
    )
    log.info("Found EMN files ${emnFileNames.size} file(s) in ${resourceDirectory}.")

    val result = EmnSchemaGenerationService().generate(EmnSchemaGenerationRequest(
      emnFiles = emnFileNames.map { File(resourceDirectory, it) },
      rootPackageName = rootPackageName,
      outputDirectory = outputDirectory,
      annotationEnhancers = annotationEnhancers.toList()
    ))

    result.generatedFiles.forEach { file ->
      file.writeText(Formatter.format(EmnKotlinAxon5MavenPlugin.KTFMT_FORMAT, file.readText()))
      log.info("Generating JSON Schema Kotlin: $file")
    }
  }
}
