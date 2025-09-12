@file:OptIn(ExperimentalKotlinPoetApi::class)

package io.holixon.emn.generation

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.format.FORMAT_KCLASS
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.format.FORMAT_LITERAL
import io.toolisticon.kotlin.generation.builder.KotlinAnnotationSpecBuilder.Companion.member
import io.toolisticon.kotlin.generation.poet.CodeBlockBuilder.Companion.codeBlock
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpec
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpecSupplier
import io.toolisticon.kotlin.generation.support.CodeBlockArray
import io.toolisticon.kotlin.generation.support.CodeBlockArray.Companion.kclassArray
import org.axonframework.commandhandling.annotation.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotations.EventTag
import org.axonframework.modelling.annotation.InjectEntity
import org.axonframework.modelling.annotation.TargetEntityId
import kotlin.reflect.KClass

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

fun TypeName.simpleName(): String {
  return if (this is ClassName) {
    this.simpleName
  } else {
    this.toString()
  }
}
