package io.holixon.emn.example.university.faculty.write.createcourse

import io.holixon.emn.example.university.faculty.type.course.CourseId
import org.axonframework.extension.kotlin.eventsourcing.EventSourcedEntityModuleExt.autodetected
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer


fun EventSourcingConfigurer.configureCreateCourse(): EventSourcingConfigurer {
  val stateEntity = autodetected<CourseId, CreateCourseCommandHandler.State>()

  val commandHandlingModule = CommandHandlingModule
    .named("CreateCourse")
    .commandHandlers()
    .autodetectedCommandHandlingComponent { CreateCourseCommandHandler() }

  return this.registerEntity(stateEntity)
    .registerCommandHandlingModule(commandHandlingModule)
}
