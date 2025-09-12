package io.holixon.emn.example.faculty.write.renamecourse

import io.holixon.emn.example.faculty.CourseCapacityChanged
import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CourseRenamed
import io.holixon.emn.example.faculty.RenameCourse
import io.holixon.emn.example.faculty.StudentSubscribedToCourse
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator

class RenameCourseState @EntityCreator constructor(): RenameCourseCommandHandler.State {
  override fun apply(event: CourseCreated): RenameCourseCommandHandler.State {
    TODO("Not yet implemented")
  }

  override fun apply(event: CourseRenamed): RenameCourseCommandHandler.State {
    TODO("Not yet implemented")
  }

  override fun apply(event: CourseCapacityChanged): RenameCourseCommandHandler.State {
    TODO("Not yet implemented")
  }

  override fun apply(event: StudentSubscribedToCourse): RenameCourseCommandHandler.State {
    TODO("Not yet implemented")
  }

  override fun decide(command: RenameCourse): List<Any> {
    TODO("Not yet implemented")
  }

}
