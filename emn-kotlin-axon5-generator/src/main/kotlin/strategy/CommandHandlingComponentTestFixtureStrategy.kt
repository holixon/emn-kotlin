package io.holixon.emn.generation.strategy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.MemberName
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator.Tags.TestFileSpec
import io.holixon.emn.generation.getEmbeddedJsonValueAsMap
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
import io.toolisticon.kotlin.avro.model.SchemaType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFun
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.classBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.builder.KotlinFunSpecBuilder
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy

@OptIn(ExperimentalKotlinPoetApi::class)
class CommandHandlingComponentTestFixtureStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, CommandSlice>(
  contextType = EmnGenerationContext::class,
  inputType = CommandSlice::class
) {

  // FIXME -> pass over via context.
  private val objectMapper = ObjectMapper().registerKotlinModule()

  override fun invoke(
    context: EmnGenerationContext,
    input: CommandSlice
  ): KotlinFileSpecList {
    val fileBuilder = fileBuilder(input.commandHandlerFixtureTestClassName)
      .addTag(TestFileSpec)

    val commandHandlerTypeBuilder = classBuilder(input.commandHandlerFixtureTestClassName).apply {

      addConstructorProperty( "fixture", ClassName("org.axonframework.test.fixture", "AxonTestFixture") )

      context.definitions.specifications
        .filter { spec -> spec.slice != null && spec.slice!!.id == input.slice.id }
        .forEach { spec ->

          val givenStage = requireNotNull(spec.givenStage) { "Given stage must be present" }
          val whenStage = requireNotNull(spec.whenStage) { "When stage must be present" }
          val thenStage = requireNotNull(spec.thenStage) { "Then stage must be present" }

          this.addKdoc("\n${spec.name}\n\n")
          addFunction(buildFun(spec.testMethodName) {
            addAnnotation(buildAnnotation(ClassName("org.junit.jupiter.api", "Test")))

            addStatement("fixture\n")
            addStatement(".given()\n")
            val givenEvents = givenStage.values.events()
            if (givenEvents.isEmpty()) {
              addStatement(".noPriorActivity()\n")
            } else {
              givenEvents.forEach { event ->
                addStatement(".event(\n") // FIXME -> this is a hack
                messageInstantiation(
                  event,
                  event.typeReference.resolveAvroPoetType(context.protocolDeclarationContext),
                  context.protocolDeclarationContext.avroPoetTypes
                )
                addStatement(")\n")
              }
            }

            addStatement(".`when`()\n")
            val commands = whenStage.values.commands()
            require(commands.size == 1) { "Currently when stage requires exactly one command." }
            val command = commands.first()
            addStatement(".command(\n") // FIXME -> this is a hack
            messageInstantiation(
              command,
              command.typeReference.resolveAvroPoetType(context.protocolDeclarationContext),
              context.protocolDeclarationContext.avroPoetTypes
            )
            addStatement(")\n")


            addStatement(".then()\n")
            val thenEvents = thenStage.values.events()
            val thenErrors = thenStage.values.errors()
            if (thenErrors.isEmpty()) {
              addStatement(".success()\n")
              if (thenEvents.isEmpty()) {
                addStatement(".noEvents()\n")
              } else {
                addStatement(".events(\n") // FIXME -> this is a hack
                thenEvents.forEach { event ->
                  messageInstantiation(
                    event,
                    event.typeReference.resolveAvroPoetType(context.protocolDeclarationContext),
                    context.protocolDeclarationContext.avroPoetTypes

                  )
                }
                addStatement(")\n")
              }
            } else {
              addStatement(".exception(%T::class.java)", IllegalStateException::class.java) // FIXME -> generate custom error types first
            }
          })
        }
    }

    fileBuilder.addType(commandHandlerTypeBuilder.build())
    return KotlinFileSpecList(fileBuilder.build())
  }

  private fun KotlinFunSpecBuilder.messageInstantiation(event: FlowElement, avroPoetType: AvroPoetType, avroPoetTypes: AvroPoetTypes) {
    val elementValue = requireNotNull(event.value) { "Element $event must have a value."}
    val propertiesMap = elementValue.getEmbeddedJsonValueAsMap(objectMapper)
    requireNotNull(propertiesMap) { "Could not parse value of $event as a map of properties." }

    // Build parameter statements
    val paramStatements = mutableListOf<String>()

    addStatement("%T(", avroPoetType.typeName) // FIXME -> HACK
    // Iterate through entries and build parameter statements
    propertiesMap.entries.forEachIndexed { index, (key, value) ->
      val avroSchemaField = avroPoetType.avroType.schema.fields.find { it.name.value == key }
      if (avroSchemaField != null) {
        if (avroSchemaField.type is SchemaType.RECORD) {
          val complexType = requireNotNull(avroPoetTypes[avroSchemaField.schema.hashCode]) { "Could not find type for ${avroSchemaField.name} for property $key" }
          addStatement(
            "$key=%T(\"$value\")", // FIXME -> this should be constructor invocation, nut just a type name
            complexType.typeName,
          )
        } else {
          // simple type, try to assign directly
          val simpleAssignment = when (value) {
            is String -> "$key=\"$value\""
            else -> "$key=$value"
          }
          addStatement(simpleAssignment)
        }
      }
      // If it's the last element, check if we need to add a comma
      if (index < propertiesMap.entries.size - 1) {
        // Not the last element, we'll add a comma in the joinToString
        addStatement(", ")
      }
    }
    addStatement(")\n")
  }

}
