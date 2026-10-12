package io.holixon.emn.example.faculty.write.renamecourse

import io.holixon.emn.example.faculty.CourseId
import org.axonframework.extension.kotlin.eventsourcing.EventSourcedEntityModuleExt.autodetected
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer

fun EventSourcingConfigurer.configureRenameCourse(): EventSourcingConfigurer {
  val stateEntity = autodetected<CourseId, RenameCourseState>()

  val commandHandlingModule = CommandHandlingModule
    .named("RenameCourse")
    .commandHandlers()
    .autodetectedCommandHandlingComponent { RenameCourseCommandHandler() }

  return this.registerCommandHandlingModule(commandHandlingModule)
    .registerEntity(stateEntity)
}
