package io.holixon.emn.example.university.infrastructure

import jakarta.annotation.Nonnull
import org.axonframework.configuration.Configuration
import org.axonframework.eventsourcing.EventSourcedEntityFactory
import org.axonframework.eventsourcing.annotation.EventSourcedEntityFactoryDefinition
import org.axonframework.eventsourcing.annotation.reflection.AnnotationBasedEventSourcedEntityFactory
import org.axonframework.messaging.MessageTypeResolver
import org.axonframework.messaging.annotation.ParameterResolverFactory

class SealedClassEventSourcedFactoryDefinition : EventSourcedEntityFactoryDefinition<Any, Any> {


  override fun createFactory(
    @Nonnull entityType: Class<Any>,
    @Nonnull entitySubTypes: Set<Class<out Any>>,
    @Nonnull idType: Class<Any>,
    @Nonnull configuration: Configuration
  ): EventSourcedEntityFactory<Any, Any> {

    val subTypes = if (entityType.isSealed) {
      entitySubTypes + entityType.permittedSubclasses
    } else {
      entitySubTypes
    }

    return AnnotationBasedEventSourcedEntityFactory(
      entityType,
      idType,
      subTypes,
      configuration.getComponent(ParameterResolverFactory::class.java),
      configuration.getComponent(MessageTypeResolver::class.java)
    )
  }


}
