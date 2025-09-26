package io.holixon.emn.example.faculty.write.createcourse

import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CreateCourse
import io.holixon.emn.example.faculty.DuplicateCourse
import org.axonframework.eventsourcing.annotations.reflection.EntityCreator

class CreateCourseState @EntityCreator constructor() : CreateCourseCommandHandler.State {
  private var created: Boolean = false

  override fun decide(command: CreateCourse): List<Any> {
    if (created) {
      throw DuplicateCourse("Course with id=${command.courseId.value} already exists.")
    }
    return listOf(CourseCreated(command.courseId, command.name, command.capacity))
  }

  override fun evolve(event: CourseCreated): CreateCourseCommandHandler.State = apply { created = true }
}
