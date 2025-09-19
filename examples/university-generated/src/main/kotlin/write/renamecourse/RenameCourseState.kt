package io.holixon.emn.example.faculty.write.renamecourse

import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CourseRenamed
import io.holixon.emn.example.faculty.RenameCourse
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator

class RenameCourseState @EntityCreator constructor(): RenameCourseCommandHandler.State {
  private var created: Boolean = false
  private var name: String = ""

  override fun apply(event: CourseRenamed): RenameCourseCommandHandler.State {
    this.created = true
    this.name = event.name
    return this
  }

  override fun decide(command: RenameCourse): List<Any> {
    return if (!this.created) {
      throw IllegalStateException("Course with given id does not exist")
    } else {
      if (command.name == name) {
        listOf()
      } else {
        listOf(CourseRenamed(command.courseId, command.name))
      }
    }
  }

  @EventSourcingHandler
  fun apply(event: CourseCreated): RenameCourseCommandHandler.State {
    this.created = true
    this.name = event.name
    return this
  }

}
