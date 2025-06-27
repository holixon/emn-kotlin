package io.holixon.emn.example.university.faculty

import io.holixon.emn.example.university.faculty.write.createcourse.configureCreateCourse
import io.holixon.emn.example.university.faculty.write.renamecourse.configureRenameCourse
import io.holixon.emn.example.university.faculty.write.renamecoursepolymorph.configureRenameCoursePolymorph
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer

fun EventSourcingConfigurer.configureFacultyModule(): EventSourcingConfigurer {
  return this
    .configureCreateCourse()
    .configureRenameCourse()
    .configureRenameCoursePolymorph()
}
