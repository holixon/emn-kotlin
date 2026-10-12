package io.holixon.emn.generation.specification

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.model.Specification
import io.holixon.emn.generation.model.Specification.Stage.ThenStage.ThenEmpty
import io.holixon.emn.generation.model.Specification.Stage.ThenStage.ThenError
import io.holixon.emn.generation.model.Specification.Stage.ThenStage.ThenEvents
import io.holixon.emn.generation.schema.EmnJsonSchemaExtractor
import io.holixon.emn.generation.schema.KotlinIdentifier
import io.holixon.emn.model.commands
import io.holixon.emn.verify.annotation.EmnSpecification
import java.io.File

data class EmnSpecificationTestGenerationRequest(
  val emnFiles: List<File>,
  val outputDirectory: File,
  val rootPackageName: String,
  val fixtureProviderClassName: String,
  val errorVerifierClassName: String? = null
)

data class EmnSpecificationTestGenerationResult(val generatedFiles: List<File>)

/** Build-tool-neutral Axon 5 fixture-test generator for EMN specifications backed by JSON Schema. */
class EmnSpecificationTestGenerationService(
  private val parser: EmnDocumentParser = EmnDocumentParser(),
  private val extractor: EmnJsonSchemaExtractor = EmnJsonSchemaExtractor(),
  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
) {

  fun generate(request: EmnSpecificationTestGenerationRequest): EmnSpecificationTestGenerationResult {
    val rootPackage = KotlinIdentifier.requirePackageName(request.rootPackageName)
    val fixtureProvider = requireQualifiedClassName(request.fixtureProviderClassName, "fixture provider class")
    val errorVerifier = request.errorVerifierClassName
      ?.takeIf(String::isNotBlank)
      ?.let { requireQualifiedClassName(it, "error verifier class") }
    request.emnFiles.forEach { require(it.isFile) { "EMN file does not exist: $it" } }
    request.outputDirectory.mkdirs()

    val generatedFiles = request.emnFiles.flatMap { emnFile ->
      val definitions = parser.parseDefinitions(emnFile)
      val renderer = JsonSchemaMessageValueRenderer(
        schemas = extractor.extract(definitions, emnFile.path),
        rootPackageName = rootPackage,
        objectMapper = objectMapper
      )
      definitions.timelines.flatMap { timeline ->
        timeline.sliceSet.mapNotNull { slice ->
          val command = slice.flowElements.commands().singleOrNull() ?: return@mapNotNull null
          val specifications = definitions.specifications
            .filter { it.slice?.id == slice.id }
            .map { Specification(it) }
          if (specifications.isEmpty()) return@mapNotNull null

          val sliceName = slice.name ?: command.typeReference.name
          val classSimpleName = pascalCase(sliceName) + "CommandHandlerTest"
          val packageName = KotlinIdentifier.requirePackageName("$rootPackage.write.${packageSegment(sliceName)}")
          val className = ClassName(packageName, classSimpleName)
          val fixtureType = ClassName("org.axonframework.test.fixture", "AxonTestFixture")
          val axonExtension = ClassName("org.axonframework.test.extension", "AxonFrameworkExtension")
          val providedFixture = ClassName("org.axonframework.test.extension", "ProvidedAxonTestFixture")
          val testClass = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.PRIVATE)
            .addAnnotation(
              com.squareup.kotlinpoet.AnnotationSpec.builder(
                ClassName("org.junit.jupiter.api.extension", "ExtendWith")
              ).addMember("%T::class", axonExtension).build()
            )
            .addAnnotation(
              com.squareup.kotlinpoet.AnnotationSpec.builder(providedFixture)
                .addMember("%T::class", fixtureProvider)
                .build()
            )
            .apply {
              specifications.forEach { addFunction(testFunction(it, renderer, fixtureType, errorVerifier)) }
              if (errorVerifier == null && specifications.any { it.thenStage is ThenError }) {
                addFunction(defaultErrorVerifierFunction())
                addFunction(defaultErrorDetailsAssertionFunction())
              }
            }
            .build()
          val fileSpec = FileSpec.builder(className).addType(testClass).build()
          fileSpec.writeTo(request.outputDirectory)
          File(request.outputDirectory, packageName.replace('.', File.separatorChar)).resolve("$classSimpleName.kt")
        }
      }
    }
    return EmnSpecificationTestGenerationResult(generatedFiles)
  }

  private fun testFunction(
    specification: Specification,
    renderer: JsonSchemaMessageValueRenderer,
    fixtureType: ClassName,
    errorVerifier: ClassName?
  ): FunSpec = FunSpec.builder(specification.testMethodName)
    .addAnnotation(ClassName("org.junit.jupiter.api", "Test"))
    .addAnnotation(
      com.squareup.kotlinpoet.AnnotationSpec.builder(EmnSpecification::class)
        .addMember("id = %S", specification.id)
        .build()
    )
    .addParameter("fixture", fixtureType)
    .addKdoc("%L\n", specification.name)
    .apply {
      specification.scenario?.let { addKdoc("\nScenario: %L\n", it) }
      addCode("fixture\n  .given()")
      if (specification.givenStage.isEmpty()) {
        addCode("\n  .noPriorActivity()")
      } else {
        specification.givenStage.forEach { event -> addCode("\n  .event(%L)", renderer.render(event)) }
      }
      addCode("\n  .`when`()\n  .command(%L)\n  .then()", renderer.render(specification.whenStage.command))
      when (val thenStage = specification.thenStage) {
        ThenEmpty -> addCode("\n  .success()\n  .noEvents()")
        is ThenEvents -> {
          addCode("\n  .success()\n  .events(")
          thenStage.map(renderer::render).forEachIndexed { index, event ->
            if (index > 0) addCode(", ")
            addCode(event)
          }
          addCode(")")
        }
        is ThenError -> {
          val errorTypeId = thenStage.error.typeReference.id
          KotlinIdentifier.requireClassName(errorTypeId, "error TypeId")
          val expectedDetails = renderer.renderErrorDetails(thenStage.error)
          addCode("\n  .noEvents()")
          addCode("\n  .exceptionSatisfies { exception ->")
          if (errorVerifier == null) {
            addCode("\n    verifyEmnError(%S, %L, exception)", errorTypeId, expectedDetails)
          } else {
            addCode("\n    %T.verify(%S, %L, exception)", errorVerifier, errorTypeId, expectedDetails)
          }
          addCode("\n  }")
        }
      }
      addCode("\n")
    }
    .build()

  private fun defaultErrorVerifierFunction(): FunSpec = FunSpec.builder("verifyEmnError")
    .addModifiers(KModifier.PRIVATE)
    .addParameter("errorTypeId", String::class)
    .addParameter("expectedDetails", MAP_STRING_ANY_NULLABLE)
    .addParameter("exception", Throwable::class)
    .addCode(
      """
      val commandException = exception as? %T
        ?: throw AssertionError("Expected CommandExecutionException for EMN error '${'$'}errorTypeId', but got ${'$'}{exception::class.qualifiedName}")
      val actualDetails = commandException.getDetails(Map::class.java).orElseThrow {
        AssertionError("CommandExecutionException for EMN error '${'$'}errorTypeId' has no structured details")
      }
      assertEmnErrorDetails(expectedDetails, actualDetails, errorTypeId)
      """.trimIndent(),
      ClassName("org.axonframework.messaging.commandhandling", "CommandExecutionException")
    )
    .build()

  private fun defaultErrorDetailsAssertionFunction(): FunSpec = FunSpec.builder("assertEmnErrorDetails")
    .addModifiers(KModifier.PRIVATE)
    .addParameter("expected", ANY_NULLABLE)
    .addParameter("actual", ANY_NULLABLE)
    .addParameter("path", String::class)
    .addCode(
      """
      when (expected) {
        is Map<*, *> -> {
          val actualMap = actual as? Map<*, *>
            ?: throw AssertionError("Expected structured details at '${'$'}path', but got ${'$'}actual")
          expected.forEach { (key, expectedValue) ->
            if (!actualMap.containsKey(key)) {
              throw AssertionError("Missing structured detail '${'$'}path.${'$'}key'")
            }
            assertEmnErrorDetails(expectedValue, actualMap[key], "${'$'}path.${'$'}key")
          }
        }
        is Set<*> -> %M(expected, actual, "Unexpected structured detail set at '${'$'}path'")
        is Iterable<*> -> {
          val actualValues = (actual as? Iterable<*>)?.toList()
            ?: throw AssertionError("Expected detail collection at '${'$'}path', but got ${'$'}actual")
          val expectedValues = expected.toList()
          if (expectedValues.size != actualValues.size) {
            throw AssertionError("Expected ${'$'}{expectedValues.size} values at '${'$'}path', but got ${'$'}{actualValues.size}")
          }
          expectedValues.indices.forEach { index ->
            assertEmnErrorDetails(expectedValues[index], actualValues[index], "${'$'}path[${'$'}index]")
          }
        }
        else -> %M(expected, actual, "Unexpected structured detail at '${'$'}path'")
      }
      """.trimIndent(),
      MemberName("org.junit.jupiter.api.Assertions", "assertEquals"),
      MemberName("org.junit.jupiter.api.Assertions", "assertEquals")
    )
    .build()

  private fun pascalCase(value: String): String = value.split(Regex("[^A-Za-z0-9]+"))
    .filter(String::isNotBlank)
    .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    .also { KotlinIdentifier.requireClassName(it, "generated specification test class name") }

  private fun packageSegment(value: String): String = value.filter(Char::isLetterOrDigit).lowercase().let {
    require(it.isNotBlank() && it.first().isLetter()) { "Cannot derive Kotlin package segment from slice name '$value'." }
    it
  }

  private fun requireQualifiedClassName(value: String, description: String): ClassName {
    val separator = value.lastIndexOf('.')
    require(separator > 0 && separator < value.lastIndex) {
      "$description must be a fully qualified class name: '$value'."
    }
    val packageName = KotlinIdentifier.requirePackageName(value.substring(0, separator))
    val simpleName = KotlinIdentifier.requireClassName(value.substring(separator + 1), description)
    return ClassName(packageName, simpleName)
  }

  companion object {
    private val ANY_NULLABLE = com.squareup.kotlinpoet.ANY.copy(nullable = true)
    private val MAP_STRING_ANY_NULLABLE = com.squareup.kotlinpoet.MAP.parameterizedBy(
      com.squareup.kotlinpoet.STRING,
      ANY_NULLABLE
    )
  }
}
