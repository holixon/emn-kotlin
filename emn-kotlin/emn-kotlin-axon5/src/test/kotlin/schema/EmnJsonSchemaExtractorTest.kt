package io.holixon.emn.generation.schema

import _ktx.ResourceKtx.resourceUrl
import io.holixon.emn.EmnDocumentParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmnJsonSchemaExtractorTest {

  private val parser = EmnDocumentParser()
  private val extractor = EmnJsonSchemaExtractor()

  @Test
  fun `extracts embedded json schemas from schema definitions command event query and error types`() {
    val definitions = parser.parseDefinitions(resourceUrl("json-schema/faculty.emn"))

    val schemas = extractor.extract(definitions)

    assertThat(schemas.map { it.typeId }).containsExactly(
      "CourseType",
      "CreateCourse",
      "CourseCreated",
      "CourseOverview"
    )
    assertThat(schemas.map { it.typeKind }).containsExactly(
      EmnSchemaTypeKind.SCHEMA_DEFINITION,
      EmnSchemaTypeKind.COMMAND,
      EmnSchemaTypeKind.EVENT,
      EmnSchemaTypeKind.QUERY
    )
    assertThat(schemas.map { it.typeId }).doesNotContain("AdminCourseView")
  }

  @Test
  fun `extracts common and payload schemas from faculty json schema diagram`() {
    val definitions = parser.parseDefinitions(resourceUrl("faculty-json-schema/faculty.emn"))

    val schemas = extractor.extract(definitions)

    assertThat(schemas.map { it.typeId }).contains(
      "CourseId",
      "StudentId",
      "CommandType_create_course",
      "EventType_course_created",
      "QueryType_courses",
      "CommandType_enroll_student",
      "EventType_student_enrolled",
      "QueryType_students",
      "CommandType_rename_course",
      "EventType_course_renamed",
      "ErrorType_duplicate_course",
      "ErrorType_course_does_not_exist"
    )
    assertThat(schemas.filter { it.typeKind == EmnSchemaTypeKind.ERROR }).hasSize(2)
  }
}
