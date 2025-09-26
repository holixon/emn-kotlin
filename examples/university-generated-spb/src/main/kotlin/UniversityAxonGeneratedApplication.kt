package io.holixon.emn.example.faculty

import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.example.faculty.write.createcourse.configureCreateCourse
import io.holixon.emn.example.faculty.write.renamecourse.configureRenameCourse
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.configuration.ApplicationConfigurer
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

val logger = KotlinLogging.logger {}

fun main(args: Array<String>) = runApplication<UniversityAxonGeneratedApplication>(*args).let {}

@SpringBootApplication
class UniversityAxonGeneratedApplication {

  @Bean
  fun configurer(): ApplicationConfigurer {
    return EventSourcingConfigurer
      .create()
      .configureCreateCourse()
      .configureRenameCourse()
  }

  @Component
  class MySampleApplicationRunner(private val commandGateway: CommandGateway) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
      logger.info { "Starting application" }

      try {
        val courseId = CourseId.random()
        val createCourse = CreateCourse(courseId, "Event Sourcing in Practice", 3)
        val renameCourse = RenameCourse(courseId, "Advanced Event Sourcing")

        commandGateway.sendAndWait(createCourse)
        commandGateway.sendAndWait(renameCourse)
        logger.info { "Successfully executed sample commands" }

        /*
        val studentId = StudentId.random()
        val enrollStudent = EnrollStudentInFaculty(studentId, "Kermit", "The Frog")
        commandGateway.sendAndWait(enrollStudent)
        */

      } catch (e: Exception) {
        logger.error(e) { "Error while executing sample commands: " + e.message }
      }
    }
  }


}
