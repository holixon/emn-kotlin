package io.holixon.emn.maven

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.DefaultEmnAxon5GeneratorProperties
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator.Tags.TestFileSpec
import io.toolisticon.kotlin.avro.AvroParser
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.spi.load
import io.toolisticon.kotlin.generation.spec.KotlinFileSpec
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import java.io.File

@OptIn(ExperimentalKotlinPoetApi::class)
@Mojo(
  name = "generate-axon-components",
  defaultPhase = LifecyclePhase.GENERATE_SOURCES,
  requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
  requiresProject = true,
)
class GenerateAxonComponentsMojo : AbstractEmnMojo() {

  @Parameter(
    property = "emn.axonComponents.outputDirectory",
    required = true,
    defaultValue = $$"${project.build.directory}/generated-sources/emn-kotlin",
  )
  private lateinit var outputDirectory: File

  @Parameter(
    property = "emn.axonComponents.testOutputDirectory",
    required = true,
    defaultValue = $$"${project.build.directory}/generated-test-sources/emn-kotlin",
  )
  private lateinit var testOutputDirectory: File

  @Parameter(property = "emn.axonComponents.emnName", required = true, defaultValue = "faculty")
  private lateinit var emnName: String

  @Parameter(
    property = "emn.rootPackageName",
    required = true,
    defaultValue = "io.holixon.emn.example.faculty",
  )
  private lateinit var rootPackageName: String

  @Parameter(property = "emn.axonComponents.instanceCreator", required = true, defaultValue = "instancio")
  private lateinit var instanceCreator: String

  @Parameter(property = "emn.axonComponents.generateConcreteStateImpl", defaultValue = "false")
  private var generateConcreteStateImpl: Boolean = false

  @Parameter(property = "emn.axonComponents.annotateForSpringBoot", defaultValue = "false")
  private var annotateForSpringBoot: Boolean = false

  @Parameter(property = "emn.axonComponents.generateCommandSlices", defaultValue = "true")
  private var generateCommandSlices: Boolean = true

  @Parameter(property = "emn.axonComponents.generateCommandSliceTests", defaultValue = "true")
  private var generateCommandSliceTests: Boolean = true

  override fun execute() {
    outputDirectory.deleteRecursively()
    testOutputDirectory.deleteRecursively()
    mojoContext.mavenProject?.addCompileSourceRoot(outputDirectory.absolutePath)
    mojoContext.mavenProject?.addTestCompileSourceRoot(testOutputDirectory.absolutePath)

    val emnFiles = findEmnFiles()
    if (emnFiles.isEmpty()) return

    val properties = DefaultEmnAxon5GeneratorProperties(
      emnName = emnName,
      rootPackageName = rootPackageName,
      instanceCreator = instanceCreator,
      generateCommandSlices = generateCommandSlices,
      generateCommandSliceTests = generateCommandSliceTests,
      generateConcreteStateImpl = generateConcreteStateImpl,
      annotateForSpringBoot = annotateForSpringBoot,
    )
    val generator = EmnAxon5AvroBasedGenerator.create(load(), properties)
    val emnParser = EmnDocumentParser()
    val avroParser = AvroParser()
    val generatedFiles = emnFiles.flatMap { emnFile ->
      val avprFile = File(emnFile.parentFile, emnFile.name.replaceAfterLast(".", "avpr"))
      require(avprFile.isFile) { "Missing AVRO protocol '$avprFile' for EMN '$emnFile'." }
      generator.generate(emnParser.parseDefinitions(emnFile), avroParser.parseProtocol(avprFile))
    }

    generatedFiles.forEach { fileSpec ->
      val targetDirectory = if (fileSpec.isTest) testOutputDirectory else outputDirectory
      val file = fileSpec.get().writeTo(targetDirectory)
      log.info("Generated EMN Axon component: $file")
    }
  }

  private val KotlinFileSpec.isTest: Boolean get() = tag(TestFileSpec::class) != null
}
