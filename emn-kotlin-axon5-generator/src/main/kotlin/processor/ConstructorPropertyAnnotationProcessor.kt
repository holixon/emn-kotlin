package io.holixon.emn.generation.processor

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.MemberName
import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.generation.conflictingAggregatesFound
import io.holixon.emn.generation.ext.StringTransformations
import io.holixon.emn.generation.ext.StringTransformations.TO_UPPER_SNAKE_CASE
import io.holixon.emn.generation.getAvroTypeDefinitionRef
import io.holixon.emn.generation.noAggregateFoundLogger
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.CommandType
import io.holixon.emn.model.EventType
import io.holixon.emn.model.applyIfExactlyOne
import io.toolisticon.kotlin.avro.generator.processor.ConstructorPropertyFromRecordFieldProcessorBase
import io.toolisticon.kotlin.avro.generator.spi.SchemaDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordField
import io.toolisticon.kotlin.avro.value.CanonicalName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.builder.KotlinConstructorPropertySpecBuilder
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpec
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpecSupplier
import io.toolisticon.kotlin.generation.tag
import org.axonframework.eventsourcing.annotations.EventTag
import org.axonframework.modelling.annotation.TargetEntityId

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalKotlinPoetApi::class)
class ConstructorPropertyAnnotationProcessor : ConstructorPropertyFromRecordFieldProcessorBase() {
  override fun invoke(
    context: SchemaDeclarationContext,
    input: RecordField,
    builder: KotlinConstructorPropertySpecBuilder
  ): KotlinConstructorPropertySpecBuilder = builder.apply {

    val emnCtx: EmnGenerationContext = context.tag()!!
    val recordType = input.memberOf
    val emnElementType = emnCtx.getEmnType(recordType)



    when (emnElementType) {
      is EventType -> {
        val aggregateLanes = emnCtx.definitions.aggregates(emnElementType).distinct()
        aggregateLanes.applyIfExactlyOne(
          logger.noAggregateFoundLogger(emnElementType),
          logger.conflictingAggregatesFound(emnElementType)
        ) { aggregateLane ->
          var aggregateIdCanonicalName = aggregateLane.idSchema.getAvroTypeDefinitionRef()?.content?.let {
            CanonicalName.parse(it)
          }
          val aggregateName = aggregateLane.name
          if (input.schema.canonicalName == aggregateIdCanonicalName && aggregateName != null) {
            val tagClassName = KotlinCodeGeneration.className(
              packageName = emnCtx.properties.rootPackageName,
              simpleName = StringTransformations.TO_UPPER_CAMEL_CASE(emnCtx.properties.emnName + "Tags")
            )
            val tagMember = MemberName(tagClassName, TO_UPPER_SNAKE_CASE(aggregateName))
            addAnnotation(EventTagAnnotation(key = tagMember))
          }
        }
      }

      is CommandType -> {
        val aggregateLanes = emnCtx.definitions.aggregates(emnElementType).distinct()
        aggregateLanes.applyIfExactlyOne(
          logger.noAggregateFoundLogger(emnElementType),
          logger.conflictingAggregatesFound(emnElementType)
        ) { aggregateLane ->
          var aggregateIdCanonicalName = aggregateLane.idSchema.getAvroTypeDefinitionRef()?.content?.let {
            CanonicalName.parse(it)
          }
          val aggregateName = aggregateLane.name
          if (input.schema.canonicalName == aggregateIdCanonicalName && aggregateName != null) {
            addAnnotation(buildAnnotation(TargetEntityId::class))
          }
        }
      }

      else -> {
        // skipping, not event and not command
      }
    }

    addKdoc("Constructor property for field '${input.name.value}' of record type '${recordType.name.value}'.")
  }

  override fun test(context: SchemaDeclarationContext, input: Any): Boolean {
    return super.test(context, input)
  }

  @OptIn(ExperimentalKotlinPoetApi::class)
  data class EventTagAnnotation(val key: MemberName) : KotlinAnnotationSpecSupplier {
    override fun spec(): KotlinAnnotationSpec = buildAnnotation(EventTag::class) {
      addMember("key = %M", key)
    }
  }
}
