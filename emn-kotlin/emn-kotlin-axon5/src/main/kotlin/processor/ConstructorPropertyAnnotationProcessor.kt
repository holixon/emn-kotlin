package io.holixon.emn.generation.processor

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.generation.*
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.CommandType
import io.holixon.emn.model.EventType
import io.holixon.emn.model.applyIfExactlyOne
import io.toolisticon.kotlin.avro.generator.processor.ConstructorPropertyFromRecordFieldProcessorBase
import io.toolisticon.kotlin.avro.generator.spi.SchemaDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordField
import io.toolisticon.kotlin.avro.value.CanonicalName
import io.toolisticon.kotlin.generation.builder.KotlinConstructorPropertySpecBuilder
import io.toolisticon.kotlin.generation.tag

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalKotlinPoetApi::class)
class ConstructorPropertyAnnotationProcessor : ConstructorPropertyFromRecordFieldProcessorBase() {
  override fun invoke(
    context: SchemaDeclarationContext,
    input: RecordField,
    builder: KotlinConstructorPropertySpecBuilder
  ): KotlinConstructorPropertySpecBuilder = builder.apply {

    val emnContext: EmnGenerationContext = context.emnContext
    val recordType = input.memberOf

    when (val emnElementType = emnContext.getEmnType(recordType)) {
      is EventType -> {
        val aggregateLanes = emnContext.definitions.aggregates(emnElementType).distinct()
        aggregateLanes.applyIfExactlyOne(
          logger.noAggregateFoundLogger(emnElementType),
          logger.conflictingAggregatesFound(emnElementType)
        ) { aggregateLane ->
          val aggregateIdCanonicalName = aggregateLane.idSchema.getAvroTypeDefinitionRef()?.content?.let {
            CanonicalName.parse(it)
          }
          if (input.schema.canonicalName == aggregateIdCanonicalName && aggregateLane.name != null) {
            val tagMember = emnContext.resolveAggregateTagName(aggregateLane)
            addAnnotation(EventTagAnnotation(key = tagMember))
          }
        }
      }

      is CommandType -> {
        val aggregateLanes = emnContext.definitions.aggregates(emnElementType).distinct()
        aggregateLanes.applyIfExactlyOne(
          logger.noAggregateFoundLogger(emnElementType),
          logger.conflictingAggregatesFound(emnElementType)
        ) { aggregateLane ->
          val aggregateIdCanonicalName = aggregateLane.idSchema.getAvroTypeDefinitionRef()?.content?.let {
            CanonicalName.parse(it)
          }
          val aggregateName = aggregateLane.name
          if (input.schema.canonicalName == aggregateIdCanonicalName && aggregateName != null) {
            addAnnotation(TargetEntityIdAnnotation)
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

}
