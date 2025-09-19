package io.holixon.emn.example.faculty.write.createcourse

import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CreateCourse
import io.holixon.emn.example.faculty.FacultyTags.COURSE
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator

class CreateCourseState @EntityCreator constructor() : CreateCourseCommandHandler.State {
  private var created: Boolean = false

  override fun decide(command: CreateCourse): List<Any> {
    if (created) {
      return listOf()
    }
    return listOf(CourseCreated(command.courseId, command.name, command.capacity))
  }

  override fun evolve(event: CourseCreated): CreateCourseCommandHandler.State = apply { created = true }
}
