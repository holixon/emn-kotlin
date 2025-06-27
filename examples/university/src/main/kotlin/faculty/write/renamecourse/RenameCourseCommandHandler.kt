package io.holixon.emn.example.university.faculty.write.renamecourse

import io.holixon.emn.example.university.faculty.FacultyTags
import io.holixon.emn.example.university.faculty.events.CourseCreated
import io.holixon.emn.example.university.faculty.events.CourseRenamed
import io.holixon.emn.example.university.infrastructure.Initial
import io.holixon.emn.example.university.infrastructure.SealedClassEventSourcedFactoryDefinition
import org.axonframework.commandhandling.annotation.CommandHandler
import org.axonframework.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.modelling.annotation.InjectEntity

class RenameCourseCommandHandler {

  @CommandHandler
  fun handle(command: RenameCourse, @InjectEntity state: State, eventAppender: EventAppender) {
    val events = decide(command, state)
    eventAppender.append(events)
  }

  private fun decide(command: RenameCourse, state: State): List<CourseRenamed> {
    return when (state) {
      is State.NoCourse -> throw IllegalStateException("Course with given id does not exist")
      is State.NamedCourse -> {
        if (command.name == state.name) {
          listOf()
        } else {
          listOf(CourseRenamed(command.courseId, command.name))
        }
      }
    }
  }

  @EventSourcedEntity(
    tagKey = FacultyTags.COURSE_ID,
    entityFactoryDefinition = SealedClassEventSourcedFactoryDefinition::class,
  )
  sealed class State {

    abstract fun evolve(event: Any): State

    class NoCourse @EntityCreator constructor(): State() {

      init {
          println("Nothing to see")
      }

      @EventSourcingHandler
      override fun evolve(event: Any): State =
        when (event) {
          is CourseCreated -> NamedCourse(event.name)
          else -> throw IllegalArgumentException("Unknown event type $event for state $this")
        }
    }

    class NamedCourse(val name: String) : State() {
      @EventSourcingHandler
      override fun evolve(event: Any): State =
        when (event) {
          is CourseRenamed -> NamedCourse(event.name)
          else -> throw IllegalArgumentException("Unknown event type $event for state $this")
        }
    }
  }
}
