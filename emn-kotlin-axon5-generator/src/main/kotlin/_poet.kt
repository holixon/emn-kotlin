@file:OptIn(ExperimentalKotlinPoetApi::class)

package io.holixon.emn.generation

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.MemberName
import io.toolisticon.kotlin.avro.generator.poet.AvroPoetType
import io.toolisticon.kotlin.avro.generator.poet.AvroPoetTypes
import io.toolisticon.kotlin.avro.model.RecordField
import io.toolisticon.kotlin.avro.model.RecordType
import io.toolisticon.kotlin.avro.model.wrapper.AvroSchema
import io.toolisticon.kotlin.avro.model.wrapper.AvroSchemaChecks.isNullable
import io.toolisticon.kotlin.avro.model.wrapper.AvroSchemaChecks.isPrimitive
import io.toolisticon.kotlin.avro.model.wrapper.AvroSchemaChecks.isRecordType
import io.toolisticon.kotlin.avro.model.wrapper.AvroSchemaChecks.isStringType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.format.FORMAT_KCLASS
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.format.FORMAT_LITERAL
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.format.FORMAT_STRING
import io.toolisticon.kotlin.generation.poet.CodeBlockBuilder.Companion.codeBlock
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpec
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpecSupplier
import io.toolisticon.kotlin.generation.support.CodeBlockArray
import org.axonframework.commandhandling.annotation.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotations.EventTag
import org.axonframework.modelling.annotation.InjectEntity
import org.axonframework.modelling.annotation.TargetEntityId
import java.util.function.Supplier

data class InjectEntityAnnotation(val idProperty: String? = null) : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(InjectEntity::class) {
    // we need an id property for creation command handler
    idProperty?.let { addStringMember("idProperty", it) }
  }
}

fun AvroPoetType.idProperty(): String? {
  // FIXME -> find a way how to model this.
  return null
}

data class EventTagAnnotation(val key: MemberName) : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(EventTag::class) {
    addMember("key = %M", key)
  }
}

object TargetEntityIdAnnotation : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(TargetEntityId::class)
}

/**
 * @EventSourcedEntity(
 *     tagKey = FacultyTags.COURSE_ID
 * )
 */
@OptIn(ExperimentalKotlinPoetApi::class)
data class EventSourcedEntityAnnotation(val key: MemberName, val concreteTypes: List<ClassName> = listOf()) : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(EventSourcedEntity::class) {
    addMember("tagKey = %M", key)
    addMember(codeBlock("concreteTypes = $FORMAT_LITERAL", CodeBlockArray(FORMAT_KCLASS, concreteTypes).build()))
  }
}

object EventSourcingHandlerAnnotation : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(EventSourcingHandler::class)
}

object CommandHandlerAnnotation : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(CommandHandler::class)
}

fun initializeMessage(avroPoetType: AvroPoetType, avroPoetTypes: AvroPoetTypes, properties: Map<String, Any?>): CodeBlock {
  require(avroPoetType.avroType is RecordType) { "Can only initialize RecordType, but was ${avroPoetType.avroType}" }
  val recordType = avroPoetType.avroType as RecordType

  data class FieldAndValue(val field: RecordField, val value: Any?) : Supplier<CodeBlock> {

    init {
      if (value == null) {
        require(field.schema.isNullable) { "Field ${field.name} is not nullable, but value is null" }
      }
      check(field.type.schema.isRecordType || field.schema.isPrimitive) { "Field ${field.name} is of type ${field.type}, which is not supported yet" }
    }

    override fun get(): CodeBlock = if (field.type.schema.isRecordType) {
      val complexType = avroPoetTypes[field.type.hashCode]
      val complexTypeRecord = complexType.avroType as RecordType
      val constructorCall = if (complexTypeRecord.schema.fields.size == 1) {
        val constructorField = complexTypeRecord.schema.fields[0]
        CodeBlock.of("%T(${constructorField.schema.poetValueFormat()})", complexType.typeName, value)
      } else {
        if (value is Map<*, *>) {
          @Suppress("UNCHECKED_CAST")
          initializeMessage(complexType, avroPoetTypes, (value as Map<String, Any?>) )
        } else {
          throw IllegalStateException("Field ${field.name} is of type ${field.type}, which can't be initialized from value $value")
        }
      }

      codeBlock("${field.name} = %L", constructorCall)
    } else {
      codeBlock("${field.name} = ${field.schema.poetValueFormat()}", value)
    }
  }

  // Fields to values
  val blocks = recordType.fields.map { field ->
    val value = if (properties.containsKey(field.name.value)) {
      properties[field.name.value]
    } else {
      // TODO -> resolve default?
      if (field.schema.isNullable) {
        null
      } else {
        throw IllegalArgumentException("No value was supplied for field ${field.name} is of type ${field.type}")
      }
    }
    FieldAndValue(field, value).get()
  }
  return CodeBlock.builder()
    .add("%T", avroPoetType.typeName)
    .add("(")
    .addAll(blocks, CodeBlock.of(", "))
    .add(")")
    .build()
}

/**
 * Adds list of code blocks to current builder, using optional separator.
 * @param blocks blocks to add.
 * @return code block builder.
 */
fun CodeBlock.Builder.addAll(blocks: List<CodeBlock>, separator: CodeBlock? = null) = apply {
  blocks.forEachIndexed { index, block ->
    add(block)
    if (separator != null && index < blocks.size - 1) {
      add(separator)
    }
  }
}

/**
 * Returns a value format for poet template expansion.
 */
fun AvroSchema.poetValueFormat() =
  if (this.isStringType) {
    FORMAT_STRING
  } else {
    FORMAT_LITERAL
  }
