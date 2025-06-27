package io.holixon.emn.example.university.faculty.write.renamecoursepolymorph

import io.holixon.emn.example.university.faculty.FacultyTags
import io.holixon.emn.example.university.faculty.events.CourseCreated
import io.holixon.emn.example.university.faculty.events.CourseRenamed
import io.holixon.emn.example.university.faculty.write.renamecoursepolymorph.RenameCoursePolymorphCommandHandler.State.NamedCourseState
import io.holixon.emn.example.university.faculty.write.renamecoursepolymorph.RenameCoursePolymorphCommandHandler.State.NoCourseState
import io.holixon.emn.example.university.infrastructure.DecidingState
import org.axonframework.commandhandling.annotation.CommandHandler
import org.axonframework.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.modelling.annotation.InjectEntity

class RenameCoursePolymorphCommandHandler {

  @CommandHandler
  fun handle(command: RenameCourse, @InjectEntity state: State, eventAppender: EventAppender) {
    eventAppender.append(state.decide(command))
  }

  @EventSourcedEntity(
    tagKey = FacultyTags.COURSE_ID,
    concreteTypes = [NoCourseState::class, NamedCourseState::class]
  )
  sealed interface State : DecidingState<RenameCourse> {

    class NoCourseState @EntityCreator constructor() : State {
      override fun decide(command: RenameCourse): List<Any> =
        throw IllegalStateException("Course with given id does not exist")

      @EventSourcingHandler
      fun apply(event: CourseCreated) = NamedCourseState(event)
    }

    class NamedCourseState(private val name: String)  : State {

      @EntityCreator
      constructor(event: CourseCreated): this(event.name)

      override fun decide(command: RenameCourse): List<Any> {
        return if (command.name == name) {
          listOf()
        } else {
          listOf(CourseRenamed(command.courseId, command.name))
        }
      }

      @EventSourcingHandler
      fun apply(event: CourseRenamed) = NamedCourseState(event.name)
    }
  }
}
