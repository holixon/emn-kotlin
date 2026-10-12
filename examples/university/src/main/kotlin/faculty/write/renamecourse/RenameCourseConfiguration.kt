package io.holixon.emn.example.university.faculty.write.renamecourse

import io.holixon.emn.example.university.faculty.type.course.CourseId
import org.axonframework.extension.kotlin.eventsourcing.EventSourcedEntityModuleExt.autodetected
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer


fun EventSourcingConfigurer.configureRenameCourse(): EventSourcingConfigurer {
  val stateEntity = autodetected<CourseId, RenameCourseCommandHandler.State>()


  val commandHandlingModule = CommandHandlingModule
    .named("RenameCourse")
    .commandHandlers()
    .autodetectedCommandHandlingComponent { RenameCourseCommandHandler() }
  //.commandHandlingComponent(
  //    handlingComponentBuilder
  //  )
  return this.registerEntity(stateEntity)
    .registerCommandHandlingModule(commandHandlingModule)
}
