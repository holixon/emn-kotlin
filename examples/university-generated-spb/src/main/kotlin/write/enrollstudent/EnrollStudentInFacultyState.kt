package io.holixon.emn.example.faculty.write.enrollstudent

import io.holixon.emn.example.faculty.EnrollStudentInFaculty
import io.holixon.emn.example.faculty.StudentEnrolledInFaculty
import org.axonframework.eventsourcing.annotations.EventSourcingHandler
import org.axonframework.eventsourcing.annotations.reflection.EntityCreator

class EnrollStudentInFacultyState @EntityCreator constructor() {
  @EventSourcingHandler
  fun evolve(event: StudentEnrolledInFaculty) = apply {
    TODO("Not yet implemented")
  }

  fun decide(command: EnrollStudentInFaculty): List<Any> {
    TODO("Not yet implemented")
  }

}
