package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.asClassName
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator.Tags.TestFileSpec
import io.holixon.emn.generation.getEmbeddedJsonValueAsMap
import io.holixon.emn.generation.initializeMessage
import io.holixon.emn.generation.resolveAvroPoetType
import io.holixon.emn.generation.spi.CommandSlice
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.generation.spi.commandHandlerFixtureTestClassName
import io.holixon.emn.generation.testMethodName
import io.holixon.emn.model.FlowElement
import io.holixon.emn.model.commands
import io.holixon.emn.model.errors
import io.holixon.emn.model.events
import io.toolisticon.kotlin.avro.generator.poet.AvroPoetType
import io.toolisticon.kotlin.avro.generator.poet.AvroPoetTypes
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFun
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.classBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.className
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy

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
    val fileBuilder = fileBuilder(input.commandHandlerFixtureTestClassName)
      .addTag(TestFileSpec)

    fun messageInstantiation(event: FlowElement, avroPoetType: AvroPoetType, avroPoetTypes: AvroPoetTypes): CodeBlock {
      val elementValue = requireNotNull(event.value) { "Element $event must have a value." }
      val propertiesMap = elementValue.getEmbeddedJsonValueAsMap(context.objectMapper)
      requireNotNull(propertiesMap) { "Could not parse value of $event as a map of properties." }

      if (avroPoetType.typeName is ClassName) {
        val cn = avroPoetType.typeName.className()
        fileBuilder.addImport("io.jan.foo", "Bar")
      }

      return initializeMessage(avroPoetType, avroPoetTypes, propertiesMap)
    }

    val commandHandlerTypeBuilder = classBuilder(input.commandHandlerFixtureTestClassName).apply {

      addConstructorProperty("fixture", AXON_FIXTURE) { makePrivate() }

      context.definitions.specifications
        .filter { spec -> spec.slice != null && spec.slice!!.id == input.slice.id }
        .forEach { spec ->

          val givenStage = requireNotNull(spec.givenStage) { "Given stage must be present" }
          val whenStage = requireNotNull(spec.whenStage) { "When stage must be present" }
          val thenStage = requireNotNull(spec.thenStage) { "Then stage must be present" }


          addFunction(buildFun(spec.testMethodName) {
            addAnnotation(JUNIT_TEST)
            addKdoc(spec.name)

            addStatement("fixture")
            addStatement(".given()")

            val givenEvents = givenStage.values.events()

            if (givenEvents.isEmpty()) {
              addStatement(".noPriorActivity()")
            } else {
              givenEvents.forEach { event ->
                val eventCode = messageInstantiation(
                  event,
                  event.typeReference.resolveAvroPoetType(context.protocolDeclarationContext),
                  context.protocolDeclarationContext.avroPoetTypes
                )
                addStatement(".event(%L)", eventCode)
              }
            }

            addStatement(".`when`()")

            val command = whenStage.values.commands().singleOrNull() ?: throw IllegalStateException("Currently when stage requires exactly one command.")

            addStatement(".command(%L)", messageInstantiation(
              command,
              command.typeReference.resolveAvroPoetType(context.protocolDeclarationContext),
              context.protocolDeclarationContext.avroPoetTypes
            ))

            addStatement(".then()")
            val thenEvents = thenStage.values.events()
            val thenErrors = thenStage.values.errors()
            if (thenErrors.isEmpty()) {
              addStatement(".success()")
              if (thenEvents.isEmpty()) {
                addStatement(".noEvents()")
              } else {
                val thenEventsCode = thenEvents.map {
                  messageInstantiation(it, it.typeReference.resolveAvroPoetType(context.protocolDeclarationContext), context.protocolDeclarationContext.avroPoetTypes)
                }
                addStatement(".events(%L)", thenEventsCode.joinToString(separator = ","))
              }
            } else {
              val errorPoetType = try {
                thenErrors.map { it.typeReference.resolveAvroPoetType(context.protocolDeclarationContext) }
                  .single().typeName.className()
              } catch (e:Exception) {
                IllegalStateException::class.asClassName()
              }
              addStatement(".exception(%T::class.java)", errorPoetType)
            }
          })
        }
    }

    fileBuilder.addType(commandHandlerTypeBuilder.build())
    return KotlinFileSpecList(fileBuilder.build())
  }


}
