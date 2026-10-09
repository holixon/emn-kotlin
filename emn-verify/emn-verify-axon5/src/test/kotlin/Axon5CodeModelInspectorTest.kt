package io.holixon.emn.verify.axon5

import com.tngtech.archunit.core.importer.ClassFileImporter
import io.holixon.emn.verify.annotation.EmnReadModel
import io.holixon.emn.verify.annotation.EmnSpecification
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class Axon5CodeModelInspectorTest {

  @Test
  fun `discovers Axon handlers read model query IDs and specifications`() {
    val classes = ClassFileImporter().importClasses(
      CourseComponent::class.java,
      CourseProjection::class.java,
      CourseSpecification::class.java,
    )

    val actual = Axon5CodeModelInspector().inspect(classes)

    val handler = actual.commandHandlers.single()
    assertThat(handler.commandTypeId).isEqualTo("CreateCourse")
    assertThat(handler.location).contains("CourseComponent.handle")
    val readModel = actual.readModels.single()
    assertThat(readModel.queryTypeIds).containsExactlyInAnyOrder("CourseOverview", "CourseDetails")
    assertThat(readModel.consumedEventTypeIds).containsExactly("CourseCreated")
    assertThat(readModel.eventHandlers.single().location).contains("CourseProjection.on")
    val specification = actual.specifications.single()
    assertThat(specification.specificationId).isEqualTo("spec-create-course")
    assertThat(specification.location).contains("CourseSpecification.createsCourse")
  }

  @Test
  fun `verifies a parsed EMN model against compiled classes end to end`() {
    val emnFile = File(requireNotNull(javaClass.getResource("/complete-model.emn")).toURI())
    val testClasses = File(requireNotNull(CourseComponent::class.java.protectionDomain.codeSource).location.toURI())

    val report = Axon5EmnVerificationService().verify(Axon5EmnVerificationRequest(
      emnFiles = listOf(emnFile),
      classDirectories = listOf(testClasses),
    ))

    assertThat(report.issues).isEmpty()
  }

  @Test
  fun `reports summaries and debug mappings with code locations`() {
    val emnFile = File(requireNotNull(javaClass.getResource("/complete-model.emn")).toURI())
    val testClasses = File(requireNotNull(CourseComponent::class.java.protectionDomain.codeSource).location.toURI())

    val result = Axon5EmnVerificationService().evaluate(Axon5EmnVerificationRequest(
      emnFiles = listOf(emnFile),
      classDirectories = listOf(testClasses),
    ))

    assertThat(result.summaryLines()).containsExactly(
      "EMN model: 1 command(s), 1 event(s), 2 query type(s), 1 specification(s).",
      "Code inspection: 1 command handler(s), 1 event handler(s), 1 read model(s), 1 specification implementation(s).",
    )
    assertThat(result.debugLines()).anyMatch { it.startsWith("COMMAND CreateCourse -> ") && it.contains("CourseComponent.handle") }
    assertThat(result.debugLines()).anyMatch { it.startsWith("EVENT CourseCreated -> ") && it.contains("CourseProjection.on") }
    assertThat(result.debugLines()).contains("QUERY CourseOverview -> io.holixon.emn.verify.axon5.CourseProjection")
    assertThat(result.debugLines()).anyMatch { it.startsWith("SPECIFICATION spec-create-course -> ") && it.contains("CourseSpecification.createsCourse") }
  }

  @Test
  fun `rejects a configured EMN file that does not exist`() {
    val exception = assertThrows<IllegalArgumentException> {
      Axon5EmnVerificationService().evaluate(Axon5EmnVerificationRequest(
        emnFiles = listOf(File("missing.emn")),
        classDirectories = emptyList(),
      ))
    }

    assertThat(exception.message).contains("Configured EMN file(s) do not exist: missing.emn")
  }
}

private data class CreateCourse(val id: String)
private data class CourseCreated(val id: String)

private class CourseComponent {
  @CommandHandler
  fun handle(command: CreateCourse) = command.id
}

@EmnReadModel("CourseOverview", "CourseDetails")
private class CourseProjection {
  @EventHandler
  fun on(event: CourseCreated) = event.id
}

private class CourseSpecification {
  @EmnSpecification("spec-create-course")
  fun createsCourse() = Unit
}
