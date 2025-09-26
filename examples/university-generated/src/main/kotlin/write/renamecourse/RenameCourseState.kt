package io.holixon.emn.example.faculty.write.renamecourse

import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CourseDoesNotExist
import io.holixon.emn.example.faculty.CourseRenamed
import io.holixon.emn.example.faculty.RenameCourse
import org.axonframework.eventsourcing.annotations.reflection.EntityCreator

class RenameCourseState @EntityCreator constructor() : RenameCourseCommandHandler.State {
  private var created: Boolean = false
  private var name: String = ""

  override fun decide(command: RenameCourse): List<Any> {
    return if (!this.created) {
      throw CourseDoesNotExist("Course with id=${command.courseId.value} does not exist.")
    } else {
      if (command.name == name) {
        listOf()
      } else {
        listOf(
          CourseRenamed(command.courseId, command.name)
        )
      }
    }
  }

  override fun evolve(event: CourseRenamed): RenameCourseCommandHandler.State {
    this.created = true
    this.name = event.name
    return this
  }


  override fun evolve(event: CourseCreated): RenameCourseCommandHandler.State {
    this.created = true
    this.name = event.name
    return this
  }

}
