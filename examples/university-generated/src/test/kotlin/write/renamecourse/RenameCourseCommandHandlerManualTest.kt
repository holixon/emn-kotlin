package io.holixon.emn.example.faculty.write.renamecourse

import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CourseDoesNotExist
import io.holixon.emn.example.faculty.CourseId
import io.holixon.emn.example.faculty.CourseRenamed
import io.holixon.emn.example.faculty.RenameCourse
import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Not needed, just for demonstration of Axon Test Fixture usage")
internal class RenameCourseCommandHandlerManualTest(val fixture: AxonTestFixture) {

  @Test
  fun givenNotExistingCourse_WhenRenameCourse_ThenException() {
    val courseId = CourseId.random()

    fixture.given()
      .noPriorActivity()
      .`when`()
      .command(RenameCourse(courseId, "Event Sourcing in Practice"))
      .then()
      .exception(CourseDoesNotExist::class.java)
      .noEvents()
  }

  @Test
  fun givenCourseCreated_WhenRenameCourse_ThenSuccess() {
    val courseId = CourseId.random()

    fixture.given()
      .event(CourseCreated(courseId, "Event Sourcing in Practice", 42))
      .`when`()
      .command(RenameCourse(courseId, "Event Sourcing in Theory"))
      .then()
      .success()
      .events(CourseRenamed(courseId, "Event Sourcing in Theory"))
  }

  @Test
  fun givenCourseCreated_WhenRenameCourseToTheSameName_ThenSuccess_NoEvents() {
    val courseId = CourseId.random()

    fixture.given()
      .event(CourseCreated(courseId, "Event Sourcing in Practice", 42))
      .`when`()
      .command(RenameCourse(courseId, "Event Sourcing in Practice"))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun givenCourseCreatedAndRenamed_WhenRenameCourse_ThenSuccess() {
    val courseId = CourseId.random()

    fixture.given()
      .event(CourseCreated(courseId, "Event Sourcing in Practice", 42))
      .event(CourseRenamed(courseId, "Event Sourcing in Theory"))
      .`when`()
      .command(RenameCourse(courseId, "Theoretical Practice of Event Sourcing"))
      .then()
      .success()
      .events(CourseRenamed(courseId, "Theoretical Practice of Event Sourcing"))
  }
}
