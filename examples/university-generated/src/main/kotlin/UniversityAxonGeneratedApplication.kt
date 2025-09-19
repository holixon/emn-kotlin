package io.holixon.emn.example.faculty

import io.holixon.emn.example.faculty.write.createcourse.configureCreateCourse
import io.holixon.emn.example.faculty.write.renamecourse.configureRenameCourse
import org.axonframework.configuration.ApplicationConfigurer
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer

class UniversityAxonGeneratedApplication {

  fun configurer(): ApplicationConfigurer {
    return EventSourcingConfigurer
      .create()
      .configureCreateCourse()
      .configureRenameCourse()
  }

}
