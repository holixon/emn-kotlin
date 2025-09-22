package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.asClassName
import io.holixon.emn.generation.*
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator.Tags.TestFileSpec
import io.holixon.emn.generation.spi.CommandSlice
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.generation.spi.commandHandlerFixtureTestClassName
import io.holixon.emn.model.FlowNode
import io.holixon.emn.model.commands
import io.holixon.emn.model.errors
import io.holixon.emn.model.events
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFun
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.classBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.className
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy

/**
 * Generates a fixture JUnit test, with passed fixture configuration as parameter.
 */
@OptIn(ExperimentalKotlinPoetApi::class)
class CommandHandlingComponentTestFixtureStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, CommandSlice>(
  contextType = EmnGenerationContext::class,
  inputType = CommandSlice::class
) {
  companion object {
    val JUNIT_TEST = buildAnnotation(ClassName("org.junit.jupiter.api", "Test"))
    val AXON_FIXTURE = ClassName("org.axonframework.test.fixture", "AxonTestFixture")
    val AXON_SETUP = ClassName("org.axonframework.test.fixture", "AxonTestPhase", "SetUp")
    val AXON_GIVEN = ClassName("org.axonframework.test.fixture", "AxonTestPhase", "Given")
    val AXON_WHEN = ClassName("org.axonframework.test.fixture", "AxonTestPhase", "When")
    val AXON_THEN = ClassName("org.axonframework.test.fixture", "AxonTestPhase", "Then")
  }

  override fun invoke(
    context: EmnGenerationContext,
    input: CommandSlice
  ): KotlinFileSpecList {

    fun messageInstantiation(message: FlowNode): CodeBlock {
      val elementValue = requireNotNull(message.value) { "Element $message must have a value." }
      val propertiesMap = elementValue.getEmbeddedJsonValueAsMap(context.objectMapper)
      requireNotNull(propertiesMap) { "Could not parse value of $message as a map of properties." }
      val avroPoetType = message.resolveAvroPoetType(context.protocolDeclarationContext)
      return initializeMessage(avroPoetType, context.protocolDeclarationContext.avroPoetTypes, propertiesMap)
    }

    val specifications = context.specificationsForSlice(input.slice)

    if (specifications.isEmpty()) {
      // nothing to generate, no specifications for slice found
      return return KotlinFileSpecList()
    }

    val fileBuilder = fileBuilder(input.commandHandlerFixtureTestClassName)
      .addTag(TestFileSpec)
    val commandHandlerTypeBuilder = classBuilder(input.commandHandlerFixtureTestClassName).apply {

      addConstructorProperty("fixture", AXON_FIXTURE) { makePrivate() }

      specifications.forEach { spec ->

        val givenStage = requireNotNull(spec.givenStage) { "Given stage must be present, but it is missing in ${spec.name}" }
        val whenStage = requireNotNull(spec.whenStage) { "When stage must be present, but it is missing in ${spec.name}" }
        val thenStage = requireNotNull(spec.thenStage) { "Then stage must be present, but it is missing in ${spec.name}" }


        addFunction(buildFun(spec.testMethodName) {
          addAnnotation(JUNIT_TEST)
          addKdoc(
            """
              ${"\n" + spec.name}
              ${if (spec.scenario != null) "\nScenario: ${spec.scenario}" else ""}
            """.trimIndent()
          )

          addCode("fixture")
          addCode(".given()")

          val givenEvents = givenStage.values.events()

          if (givenEvents.isEmpty()) {
            addStatement(".noPriorActivity()")
          } else {
            givenEvents.forEach { event ->
              addCode(
                CodeBlock
                  .builder()
                  .add(".event(")
                  .add(messageInstantiation(event))
                  .add(")")
                  .build()
              )
            }
          }

          addCode(".`when`()")
          require(whenStage.values.commands().size == 1) { "Currently when stage requires exactly one command, but ${whenStage.values.commands().size} wre specified in ${spec.name}" }
          val command = whenStage.values.commands().single()
          addCode(
            CodeBlock.builder()
              .add(".command(")
              .add(messageInstantiation(command))
              .add(")\n")
              .build()
          )

          addStatement(".then()")
          val thenEvents = thenStage.values.events()
          val thenErrors = thenStage.values.errors()
          if (thenErrors.isEmpty()) {
            addStatement(".success()")
          } else {
            require(thenErrors.size == 1) { "At most one error is supported in the then stage, but ${thenErrors.size} were specified in ${spec.name}" }
            val errorPoetType = try {
              thenErrors
                .map { it.resolveAvroPoetType(context.protocolDeclarationContext) }
                .single()
                .typeName
                .className()
            } catch (_: Exception) {
              IllegalStateException::class.asClassName()
            }
            addStatement(".exception(%T::class.java)", errorPoetType)
          }
          if (thenEvents.isEmpty()) {
            addCode(
              CodeBlock.of(".noEvents()")
            )
          } else {
            addCode(
              CodeBlock
                .builder()
                .add(".events(")
                .addAll(thenEvents.map { event -> messageInstantiation(event) }, CodeBlock.of(",\n"))
                .add(")")
                .build()
            )
          }
        })
      }
    }

    fileBuilder.addType(commandHandlerTypeBuilder.build())
    return KotlinFileSpecList(fileBuilder.build())
  }

}
