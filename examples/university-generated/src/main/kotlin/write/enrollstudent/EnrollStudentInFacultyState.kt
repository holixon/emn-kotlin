package io.holixon.emn.example.faculty.write.enrollstudent

import io.holixon.emn.example.faculty.EnrollStudentInFaculty
import io.holixon.emn.example.faculty.StudentEnrolledInFaculty
import org.axonframework.eventsourcing.annotations.reflection.EntityCreator

class EnrollStudentInFacultyState @EntityCreator constructor() : EnrollStudentCommandHandler.State {
  override fun evolve(event: StudentEnrolledInFaculty): EnrollStudentCommandHandler.State {
    TODO("Not yet implemented")
  }

  override fun decide(command: EnrollStudentInFaculty): List<Any> {
    TODO("Not yet implemented")
  }

}
