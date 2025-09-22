package io.holixon.emn.generation.maven

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.DefaultEmnAxon5GeneratorProperties
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator
import io.holixon.emn.generation.maven.EmnKotlinAxon5MavenPlugin.writeToFormatted
import io.holixon.emn.generation.maven.GenerateMojo.Companion.GOAL
import io.toolisticon.kotlin.avro.AvroParser
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.spi.load
import io.toolisticon.kotlin.generation.spec.KotlinFileSpec
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

  /**
   * The source directory of EMN and AVRO protocol files. This directory is added to the classpath
   * at schema compiling time. All files can therefore be referenced as classpath
   * resources following the directory structure under the source directory.
   */
  @Parameter(
    property = "resourceDirectory",
    required = true,
    defaultValue = EmnKotlinAxon5MavenPlugin.DEFAULT_RESOURCES_DIRECTORY
  )
  private lateinit var resourceDirectory: File

  /**
   * The output directory will contain the final generated sources.
   */
  @Parameter(
    property = "outputDirectory",
    required = true,
    defaultValue = EmnKotlinAxon5MavenPlugin.DEFAULT_GENERATED_SOURCES
  )
  private lateinit var outputDirectory: File

  /**
   * The output directory will contain the final generated test sources.
   */
  @Parameter(
    property = "testOutputDirectory",
    required = true,
    defaultValue = EmnKotlinAxon5MavenPlugin.DEFAULT_GENERATED_TEST_SOURCES
  )
  private lateinit var testOutputDirectory: File

  /**
   * List of include patterns to use. See also [org.apache.maven.shared.model.fileset.FileSet]
   * for pattern format.
   */
  @Parameter(
    property = "includes",
  )
  private var includes : Array<String> = EmnKotlinAxon5MavenPlugin.DEFAULT_INCLUDES

  private val SPI_REGISTRY = load()

  override fun execute() {

    outputDirectory.createIfNotExists()
    mojoContext.mavenProject?.addCompileSourceRoot(outputDirectory.absolutePath)

    testOutputDirectory.createIfNotExists()
    mojoContext.mavenProject?.addTestCompileSourceRoot(testOutputDirectory.absolutePath)

    if (!resourceDirectory.exists()) {
      log.warn("Skip non existing resource directory $resourceDirectory.")
      return
    }

    // list of simple file names, eg faculty.emn
    val emnFileNames = EmnKotlinAxon5MavenPlugin.findIncludedFiles(
      absPath = resourceDirectory.absolutePath,
      includes = includes,
    )
    log.info("Found EMN files ${emnFileNames.size} file(s) in ${resourceDirectory}.")

    val filePairs = emnFileNames.map { emnFileName ->
      val emnFile = File(resourceDirectory, emnFileName)
      check(emnFile.exists()) { "File $emnFile does not exist." }
      val avprFile = File(resourceDirectory, emnFileName.replaceAfterLast(".", "avpr"))
      check(avprFile.exists()) { "Missing AVRO protocol '$avprFile' for EMN '$emnFile'." }
      emnFile to avprFile
    }

    // FIXME configurable per filePair
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

    val declarationPairs = filePairs.map { filePair ->
      emnParser.parseDefinitions(filePair.first) to avprParser.parseProtocol(filePair.second)
    }

    val fileSpecs = declarationPairs.flatMap { (definition, declaration) ->
      generator.generate(definition, declaration)
    }

    fileSpecs.filter { it.isMain }.forEach {
      val file = it.writeToFormatted(outputDirectory)
      log.info("Generating: $file")
    }
    fileSpecs.filter { it.isTest }.forEach {
      val file = it.writeToFormatted(testOutputDirectory)
      log.info("Generating Tests: $file")
    }
  }

  val KotlinFileSpec.isMain: Boolean get() = this.tag(EmnAxon5AvroBasedGenerator.Tags.TestFileSpec::class) == null
  val KotlinFileSpec.isTest: Boolean get() = this.tag(EmnAxon5AvroBasedGenerator.Tags.TestFileSpec::class) != null
}
