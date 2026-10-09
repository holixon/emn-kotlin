package io.holixon.emn.verify.axon5

import com.tngtech.archunit.core.domain.JavaAnnotation
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaCodeUnit
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import io.holixon.emn.verify.ActualCodeModel
import io.holixon.emn.verify.ActualCommandHandler
import io.holixon.emn.verify.ActualEventHandler
import io.holixon.emn.verify.ActualReadModel
import io.holixon.emn.verify.ActualSpecification
import io.holixon.emn.verify.annotation.EmnReadModel
import io.holixon.emn.verify.annotation.EmnSpecification
import java.nio.file.Path

class Axon5CodeModelInspector {

  fun inspect(classDirectories: Collection<Path>): ActualCodeModel {
    val existingDirectories = classDirectories.filter { it.toFile().isDirectory }
    if (existingDirectories.isEmpty()) return ActualCodeModel()
    return inspect(ClassFileImporter().importPaths(existingDirectories))
  }

  fun inspect(classes: JavaClasses): ActualCodeModel = ActualCodeModel(
    commandHandlers = classes.flatMap { javaClass ->
      javaClass.codeUnits
        .filter { it.isAnnotatedWith(COMMAND_HANDLER_ANNOTATION) }
        .map { codeUnit ->
          ActualCommandHandler(
            commandTypeId = handlerTypeId(codeUnit, COMMAND_HANDLER_ANNOTATION, "commandName"),
            location = codeUnit.fullName,
          )
        }
    }.sortedBy { it.location },
    readModels = classes
      .filter { it.isAnnotatedWith(EmnReadModel::class.java) }
      .map { javaClass ->
        val eventHandlers = javaClass.allMethods
          .filter { it.isAnnotatedWith(EVENT_HANDLER_ANNOTATION) }
          .map { method ->
            ActualEventHandler(
              eventTypeId = handlerTypeId(method, EVENT_HANDLER_ANNOTATION, "eventName"),
              location = method.fullName,
            )
          }.sortedBy { it.location }
        ActualReadModel(
          queryTypeIds = javaClass.getAnnotationOfType(EmnReadModel::class.java).queryTypeIds.toSet(),
          consumedEventTypeIds = eventHandlers.mapTo(sortedSetOf()) { it.eventTypeId },
          location = javaClass.name,
          eventHandlers = eventHandlers,
        )
      }.sortedBy { it.location },
    specifications = classes.flatMap { javaClass ->
      buildList {
        if (javaClass.isAnnotatedWith(EmnSpecification::class.java)) {
          add(ActualSpecification(
            specificationId = javaClass.getAnnotationOfType(EmnSpecification::class.java).id,
            location = javaClass.name,
          ))
        }
        javaClass.methods
          .filter { it.isAnnotatedWith(EmnSpecification::class.java) }
          .forEach { method ->
            add(ActualSpecification(
              specificationId = method.getAnnotationOfType(EmnSpecification::class.java).id,
              location = method.fullName,
            ))
          }
      }
    }.sortedBy { it.location },
  )

  private fun handlerTypeId(
    codeUnit: JavaCodeUnit,
    annotationTypeName: String,
    logicalNameProperty: String,
  ): String {
    val annotation = codeUnit.getAnnotationOfType(annotationTypeName)
    val logicalName = annotation.stringProperty(logicalNameProperty)
    if (!logicalName.isNullOrBlank()) return logicalName

    val explicitPayloadType = annotation.javaClassProperty("payloadType")
    if (explicitPayloadType != null && explicitPayloadType.name != Any::class.java.name) {
      return explicitPayloadType.simpleName
    }

    return requireNotNull(codeUnit.rawParameterTypes.firstOrNull()) {
      "Axon handler '${codeUnit.fullName}' must declare a payload parameter or an explicit payloadType."
    }.simpleName
  }

  private fun JavaAnnotation<*>.stringProperty(name: String): String? =
    get(name).orElse(null) as? String

  private fun JavaAnnotation<*>.javaClassProperty(name: String): JavaClass? =
    get(name).orElse(null) as? JavaClass

  companion object {
    const val COMMAND_HANDLER_ANNOTATION =
      "org.axonframework.messaging.commandhandling.annotation.CommandHandler"
    const val EVENT_HANDLER_ANNOTATION =
      "org.axonframework.messaging.eventhandling.annotation.EventHandler"
  }
}
