package io.holixon.emn.example.faculty.read.courses

import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CourseRenamed
import org.axonframework.eventhandling.annotations.EventHandler
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class Courses {

  @EventHandler
  fun on(event: CourseCreated) {
    logger.info { "[READ SIDE]: A new course is created: \n$event" }
  }

  @EventHandler
  fun on(event: CourseRenamed) {
    logger.info { "[READ SIDE]: An existing course is renamed: \n$event" }
  }
}
