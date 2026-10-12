package io.holixon.emn.example.faculty.write.createcourse

import io.holixon.emn.example.faculty.CourseId
import org.axonframework.extension.kotlin.eventsourcing.EventSourcedEntityModuleExt.autodetected
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer

fun EventSourcingConfigurer.configureCreateCourse(): EventSourcingConfigurer {
  val stateEntity = autodetected<CourseId, CreateCourseState>()

  val commandHandlingModule = CommandHandlingModule
    .named("CreateCourse")
    .commandHandlers()
    .autodetectedCommandHandlingComponent { CreateCourseCommandHandler() }

  return this.registerCommandHandlingModule(commandHandlingModule)
    .registerEntity(stateEntity)
}
