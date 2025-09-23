package io.holixon.emn.generation

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import io.toolisticon.kotlin.avro.model.wrapper.AvroSchema
import io.toolisticon.kotlin.avro.model.wrapper.AvroSchemaChecks.isNullable
import io.toolisticon.kotlin.generation.poet.CodeBlockBuilder

/**
 * Specifies the style how objects are created.
 */
interface CreatorStyle {

  fun getConstructorCodeBlockBuilder(elementTypeName: TypeName, blocks: List<CodeBlock>): CodeBlockBuilder

  fun supportsSingleValueCreation(schema: AvroSchema, value: Any?): Boolean = schema.fields.size == 1

  fun getSingleValueConstructorCodeBlockBuilder(elementTypeName: TypeName, schema: AvroSchema, value: Any?): CodeBlockBuilder
  fun validateValues(schema: AvroSchema, properties: Map<String, Any?>) {}
}

object DirectCreatorStyle : CreatorStyle {

  override fun getConstructorCodeBlockBuilder(elementTypeName: TypeName, blocks: List<CodeBlock>) =
    CodeBlockBuilder.builder()
      .add("%T", elementTypeName)
      .add("(")
      .addAll(blocks, CodeBlock.of(", "))
      .add(")")


  override fun getSingleValueConstructorCodeBlockBuilder(elementTypeName: TypeName, schema: AvroSchema, value: Any?) =
    CodeBlockBuilder.builder()
      .add("%T(${schema.fields[0].schema.poetValueFormat()})", elementTypeName, value)

  override fun validateValues(schema: AvroSchema, properties: Map<String, Any?>) {
    schema.fields.forEach { field ->
      if (!properties.containsKey(field.name.value)) {
        if (!field.schema.isNullable) {
          throw IllegalArgumentException(
            "Failed to instantiate '${schema.canonicalName.namespace.value + "." + schema.canonicalName.name.value}'. "
              + "No value was supplied for field '${field.name}' of type '${field.type.displayName}'"
          )
        }
      }
    }
  }
}

object InstancioCreatorStyle : CreatorStyle {

  override fun getConstructorCodeBlockBuilder(elementTypeName: TypeName, blocks: List<CodeBlock>): CodeBlockBuilder {
    val instancio = ClassName("org.instancio", "Instancio")
    return CodeBlockBuilder.builder()
      .add("%T.create(%T::class.java)", instancio, elementTypeName).add(".copy(")
      .addAll(blocks, CodeBlock.of(", "))
      .add(")")
  }

  override fun getSingleValueConstructorCodeBlockBuilder(
    elementTypeName: TypeName,
    schema: AvroSchema,
    value: Any?
  ) =
    CodeBlockBuilder // TODO can we be smarter than this?
      .builder()
      .add("%T(${schema.fields[0].schema.poetValueFormat()})", elementTypeName, value)

}

