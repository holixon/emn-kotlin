package io.holixon.emn.verify

import io.holixon.emn.model.CommandType
import io.holixon.emn.model.Definitions
import io.holixon.emn.model.EventType
import io.holixon.emn.model.EventSequenceType
import io.holixon.emn.model.MessageFlowType
import io.holixon.emn.model.QueryType
import io.holixon.emn.model.Specification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmnVerifierTest {

  private val createCourse = CommandType("CreateCourse", "Create course", null)
  private val courseCreated = EventType("CourseCreated", "Course created", null)
  private val courseRenamed = EventType("CourseRenamed", "Course renamed", null)
  private val courseOverview = QueryType("CourseOverview", "Course overview", null)
  private val courseDetails = QueryType("CourseDetails", "Course details", null)
  private val definitions = Definitions(
    nodeTypes = listOf(createCourse, courseCreated, courseRenamed, courseOverview, courseDetails),
    flowTypes = listOf(
      MessageFlowType("flow-1", null, courseCreated, courseOverview),
      MessageFlowType("flow-2", null, courseRenamed, courseDetails),
    ),
    timelines = emptyList(),
    specifications = listOf(Specification("spec-create-course", "Create course")),
  )
  private val expected = ExpectedEmnModelFactory.create(definitions)

  @Test
  fun `accepts complete command read model and specification coverage`() {
    val report = EmnVerifier.verify(expected, ActualCodeModel(
      commandHandlers = listOf(ActualCommandHandler("CreateCourse", "CourseHandler.handle")),
      readModels = listOf(
        ActualReadModel(
          queryTypeIds = setOf("CourseOverview", "CourseDetails"),
          consumedEventTypeIds = setOf("CourseCreated", "CourseRenamed"),
          location = "CourseProjection",
        )
      ),
      specifications = listOf(ActualSpecification("spec-create-course", "CourseTest.createsCourse")),
    ))

    assertThat(report.issues).isEmpty()
  }

  @Test
  fun `reports events outside all queries served by a read model as warnings`() {
    val report = EmnVerifier.verify(expected, ActualCodeModel(
      commandHandlers = listOf(ActualCommandHandler("CreateCourse", "CourseHandler.handle")),
      readModels = listOf(
        ActualReadModel(
          queryTypeIds = setOf("CourseOverview", "CourseDetails"),
          consumedEventTypeIds = setOf("CourseCreated", "CourseRenamed", "CourseDeleted"),
          location = "CourseProjection",
        )
      ),
      specifications = listOf(ActualSpecification("spec-create-course", "CourseTest.createsCourse")),
    ))

    assertThat(report.errors).isEmpty()
    val warning = report.warnings.single()
    assertThat(warning.code).isEqualTo(VerificationIssueCode.UNMODELED_EVENT_HANDLER)
    assertThat(warning.message).contains("CourseDeleted")
  }

  @Test
  fun `reports missing implementations`() {
    val report = EmnVerifier.verify(expected, ActualCodeModel())

    assertThat(report.errors.map { it.code }).containsExactlyInAnyOrder(
      VerificationIssueCode.MISSING_COMMAND_HANDLER,
      VerificationIssueCode.MISSING_READ_MODEL,
      VerificationIssueCode.MISSING_READ_MODEL,
      VerificationIssueCode.MISSING_SPECIFICATION,
    )
  }

  @Test
  fun `expands event sequence types connected to queries`() {
    val sequenceType = EventSequenceType(
      id = "CourseLifecycle",
      name = "Course lifecycle",
      eventTypes = listOf(courseCreated, courseRenamed),
    )
    val sequenceDefinitions = Definitions(
      nodeTypes = listOf(courseCreated, courseRenamed, sequenceType, courseOverview),
      flowTypes = listOf(MessageFlowType("sequence-flow", null, sequenceType, courseOverview)),
      timelines = emptyList(),
      specifications = emptyList(),
    )

    val sequenceExpected = ExpectedEmnModelFactory.create(sequenceDefinitions)

    assertThat(sequenceExpected.consumedEventTypeIdsByQuery["CourseOverview"])
      .containsExactly("CourseCreated", "CourseRenamed")
  }
}
