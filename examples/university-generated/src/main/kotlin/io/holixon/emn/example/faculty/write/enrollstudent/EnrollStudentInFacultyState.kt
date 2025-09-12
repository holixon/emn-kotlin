package io.holixon.emn.example.faculty.io.holixon.emn.example.faculty.write.enrollstudent

import io.holixon.emn.example.faculty.EnrollStudentInFaculty
import io.holixon.emn.example.faculty.StudentEnrolledInFaculty
import io.holixon.emn.example.faculty.write.enrollstudent.EnrollStudentCommandHandler

class EnrollStudentInFacultyState : EnrollStudentCommandHandler.State {
  override fun apply(event: StudentEnrolledInFaculty): EnrollStudentCommandHandler.State {
    TODO("Not yet implemented")
  }

  override fun decide(command: EnrollStudentInFaculty): List<Any> {
    TODO("Not yet implemented")
  }
}
