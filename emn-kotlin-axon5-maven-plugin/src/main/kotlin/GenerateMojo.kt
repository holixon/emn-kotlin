package io.holixon.emn.generation.maven

import _ktx.ResourceKtx.resourceUrl
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.DefaultEmnAxon5GeneratorProperties
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator
import io.holixon.emn.generation.maven.EmnKotlinAxon5MavenPlugin.writeToFormatted
import io.holixon.emn.generation.maven.GenerateMojo.Companion.GOAL
import io.toolisticon.kotlin.avro.AvroKotlin
import io.toolisticon.kotlin.avro.AvroParser
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.spi.load
import io.toolisticon.maven.fn.FileExt.createIfNotExists
import io.toolisticon.maven.mojo.AbstractContextAwareMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import java.io.File

@OptIn(ExperimentalKotlinPoetApi::class)
@Mojo(
  name = GOAL,
  defaultPhase = LifecyclePhase.GENERATE_SOURCES,
  requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
  requiresProject = true
)
class GenerateMojo : AbstractContextAwareMojo() {

  companion object {
    const val GOAL = "generate"
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
    defaultValue = EmnKotlinAxon5MavenPlugin.DEFAULT_GENERATED_SOURCES
  )
  private lateinit var outputDirectory: File

  private val SPI_REGISTRY = load()

  override fun execute() {

    outputDirectory.createIfNotExists()
    mojoContext.mavenProject?.addCompileSourceRoot(outputDirectory.absolutePath)

    val includes = arrayOf("**/*.avpr")

    if (!resourceDirectory.exists()) {
      log.warn("Skip non existing resource directory $resourceDirectory.")
      return
    }

    val properties = DefaultEmnAxon5GeneratorProperties(
      emnName = "faculty",
      rootPackageName = "io.holixon.emn.example.faculty",
    )

    val generator = EmnAxon5AvroBasedGenerator.create(
      SPI_REGISTRY,
      properties,
    )

    val emnParser = EmnDocumentParser()
    val avprParser = AvroParser()

    val emnFile = File(resourceDirectory, "faculty.emn")
    val avprFile = File(resourceDirectory, "faculty.avpr")

    val definitions = emnParser.parseDefinitions(emnFile)
    val declaration = avprParser.parseProtocol(avprFile)

    val fileSpecs = generator.generate(definitions, declaration)
    fileSpecs.forEach {
      val file = it.writeToFormatted(outputDirectory)
      log.info("Generating: $file")
    }
  }
}
