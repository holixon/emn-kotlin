package io.holixon.emn.example.university.faculty.write.renamecoursepolymorph

import io.holixon.emn.example.university.faculty.type.course.CourseId
import io.holixon.emn.example.university.faculty.write.renamecoursepolymorph.RenameCoursePolymorphCommandHandler.State
import org.axonframework.extension.kotlin.eventsourcing.EventSourcedEntityModuleExt.autodetected
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer


fun EventSourcingConfigurer.configureRenameCoursePolymorph(): EventSourcingConfigurer {
  val stateEntity = autodetected<CourseId, State>()


  val commandHandlingModule = CommandHandlingModule
    .named("RenameCoursePolymorph")
    .commandHandlers()
    .autodetectedCommandHandlingComponent { RenameCoursePolymorphCommandHandler() }

  //.commandHandlingComponent(
  //    handlingComponentBuilder
  //  )
  return this.registerEntity(stateEntity)
    .registerCommandHandlingModule(commandHandlingModule)
}
