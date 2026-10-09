package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.*
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator.Tags.TestFileSpec
import io.holixon.emn.generation.model.CommandSlice
import io.holixon.emn.generation.model.Specification
import io.holixon.emn.generation.model.Specification.Stage.ThenStage.*
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.FlowNode
import io.toolisticon.kotlin.avro.generator.AvroKotlinGenerator
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildClass
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFun
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.className
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy
import io.toolisticon.kotlin.generation.support.GeneratedAnnotation

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

    val specifications = context.specifications[input.slice]

    if (specifications.isEmpty()) {
      // nothing to generate, no specifications for slice found
      return KotlinFileSpecList.EMPTY
    }

    val className = input.commandHandlerFixtureTestClassName
    val fileBuilder = fileBuilder(className)
      .addTag(TestFileSpec)
      .addAnnotation(context.generatedAnnotation)

    val commandHandlerTestClass = buildClass(className) {
      addConstructorProperty("fixture", AXON_FIXTURE).makePrivate()
      specifications.forEach { addFunction(buildTestFunction(it, context)) }
    }

    return KotlinFileSpecList(fileBuilder.addType(commandHandlerTestClass).build())
  }

  fun buildTestFunction(specification: Specification, context: EmnGenerationContext) = buildFun(specification.testMethodName) {
    addAnnotation(JUNIT_TEST)
    addKdoc(specification.messageKdoc)

    addCode("fixture")

    // GIVEN
    with(specification.givenStage) {
      addCode(".given()")
      if (isEmpty()) {
        addStatement(".noPriorActivity()")
      } else {
        forEach { event ->
          addCode {
            add(".event(")
            add(messageInstantiation(event, context))
            add(")")
          }
        }
      }
    }

    // WHEN
    with(specification.whenStage) {
      addCode(".`when`()")
      addCode {
        add(".command(")
        add(messageInstantiation(command, context))
        add(")")
      }
    }

    // THEN
    with(specification.thenStage) {
      addStatement(".then()")
      when (this) {
        is ThenEmpty -> {
          addStatement(".success()")
          addStatement(".noEvents()")
        }

        is ThenEvents -> {
          addStatement(".success()")
          addCode {
            add(".events(")
            addAll(map { event -> messageInstantiation(event, context) }, CodeBlock.of(", "))
            add(")")
          }
        }

        is ThenError -> {
          addStatement(".noEvents()")

          val exceptionClass = context.avroTypes[error.errorType].poetType.typeName.className()
          val message = error.value?.getEmbeddedJsonValueAsMap(context.objectMapper)?.get("message")

          if (message != null) {
            addStatement(".exception(%T::class.java, %S)", exceptionClass, message)
          } else {
            addStatement(".exception(%T::class.java)", exceptionClass)
          }
        }
      }
    }
  }

  fun messageInstantiation(message: FlowNode, context: EmnGenerationContext): CodeBlock {
    val elementValue = requireNotNull(message.value) { "Element $message must have a value." }
    val propertiesMap = elementValue.getEmbeddedJsonValueAsMap(context.objectMapper)
    requireNotNull(propertiesMap) { "Could not parse value of $message as a map of properties." }
    val avroPoetType = context.avroTypes.messages.single { it.nodeType == message.typeReference }.poetType

    return when (context.properties.instanceCreator) {
      "instancio" -> instantiateMessageWithInstancio(avroPoetType, context.protocolDeclarationContext.avroPoetTypes, propertiesMap)
      else -> instantiateMessageDirectly(avroPoetType, context.protocolDeclarationContext.avroPoetTypes, propertiesMap)
    }
  }

  override fun test(context: EmnGenerationContext, input: Any): Boolean {
    return context.properties.generateCommandSliceTests && super.test(context, input)
  }
}
