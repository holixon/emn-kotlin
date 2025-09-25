package io.holixon.emn.generation.model

import io.holixon.emn.generation.model.Specification.Stage.GivenStage
import io.holixon.emn.generation.model.Specification.Stage.ThenStage
import io.holixon.emn.generation.model.Specification.Stage.WhenStage
import io.holixon.emn.model.Command
import io.holixon.emn.model.CommandType
import io.holixon.emn.model.Error
import io.holixon.emn.model.ErrorType
import io.holixon.emn.model.Event
import io.holixon.emn.model.EventType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import io.holixon.emn.model.Specification as EmnSpecification
import io.holixon.emn.model.GivenStage as EmnGivenStage
import io.holixon.emn.model.WhenStage as EmnWhenStage
import io.holixon.emn.model.ThenStage as EmnThenStage
import io.holixon.emn.model.*
import org.junit.jupiter.api.Disabled


class SpecificationTest {

  @Test
  fun `create from valid`() {
    val spec = EmnSpecification(
      id = "1",
      name = "Spec 1",
      givenStage = EmnGivenStage(id="g1", values = emptyList()),
      whenStage = EmnWhenStage(id="w1", values = listOf(
        Command(id = "c1", typeReference = CommandType(id = "ct1", name = "c", schema = null), value = null)
      )),
      thenStage = EmnThenStage(id="t1", values = emptyList()),
    )

    val emnSpec = Specification(spec)
    assertThat(emnSpec.id).isEqualTo(spec.id)
  }


  @Test
  fun `test testMethodName with events in given stage`() {
    // Create a specification with events in the given stage
    val event1 = Event(
      id = "event1",
      typeReference = EventType(id = "event1", name = "Event1", schema = null),
      value = null
    )
    val event2 = Event(
      id = "event2",
      typeReference = EventType(id = "event2", name = "Event2", schema = null),
      value = null
    )
    val command = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )
    val resultEvent = Event(
      id = "event3",
      typeReference = EventType(id = "event3", name = "Event3", schema = null),
      value = null
    )
    val resultEvent2 = Event(
      id = "event4",
      typeReference = EventType(id = "event4", name = "Event4", schema = null),
      value = null
    )

    val emnSpecification = EmnSpecification(
      id = "spec1",
      name = "Test Specification",
      givenStage = EmnGivenStage(
        id = "given1",
        values = listOf(
          event1,
          event2,
          // Add a non-event element to verify it's filtered out
          Command(
            id = "nonEvent",
            typeReference = CommandType(id = "nonEvent", name = "SomeElementNotEvent", schema = null),
            value = null
          )
        )
      ),
      whenStage = EmnWhenStage(
        id = "when1",
        values = listOf(command)
      ),
      thenStage = EmnThenStage(
        id = "then1",
        values = listOf(resultEvent, resultEvent2)
      )
    )

    assertThat(Specification(emnSpecification).testMethodName).isEqualTo("givenEvent1AndEvent2_whenCommand1_thenEvent3AndEvent4")
  }

  @Test
  fun `test testMethodName with events in given stage but no events in then stage`() {
    // Create a specification with events in the given stage
    val event1 = Event(
      id = "event1",
      typeReference = EventType(id = "event1", name = "Event1", schema = null),
      value = null
    )
    val event2 = Event(
      id = "event2",
      typeReference = EventType(id = "event2", name = "Event2", schema = null),
      value = null
    )
    val command = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )

    val emnSpecification = EmnSpecification(
      id = "spec1",
      name = "Test Specification",
      givenStage = EmnGivenStage(
        id = "given1",
        values = listOf(
          event1,
          event2,
          // Add a non-event element to verify it's filtered out
          Command(
            id = "nonEvent",
            typeReference = CommandType(id = "nonEvent", name = "SomeElementNotEvent", schema = null),
            value = null
          )
        )
      ),
      whenStage = EmnWhenStage(
        id = "when1",
        values = listOf(command)
      ),
      thenStage = EmnThenStage(
        id = "then1",
        values = listOf()
      )
    )

    assertThat(Specification(emnSpecification).testMethodName).isEqualTo("givenEvent1AndEvent2_whenCommand1_thenNoEvents")
  }

  @Test
  fun `test testMethodName with events in given stage and error in then stage`() {
    // Create a specification with events in the given stage
    val event1 = Event(
      id = "event1",
      typeReference = EventType(id = "event1", name = "Event1", schema = null),
      value = null
    )
    val event2 = Event(
      id = "event2",
      typeReference = EventType(id = "event2", name = "Event2", schema = null),
      value = null
    )
    val command = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )

    val error = Error(
      id = "error",
      typeReference = ErrorType(id = "error1", name = "Error1", schema = null),
      value = null
    )

    val emnSpecification = EmnSpecification(
      id = "spec1",
      name = "Test Specification",
      givenStage = EmnGivenStage(
        id = "given1",
        values = listOf(
          event1,
          event2,
          // Add a non-event element to verify it's filtered out
          Command(
            id = "nonEvent",
            typeReference = CommandType(id = "nonEvent", name = "SomeElementNotEvent", schema = null),
            value = null
          )
        )
      ),
      whenStage = EmnWhenStage(
        id = "when1",
        values = listOf(command)
      ),
      thenStage = EmnThenStage(
        id = "then1",
        values = listOf(error)
      )
    )

    assertThat(Specification(emnSpecification).testMethodName).isEqualTo("givenEvent1AndEvent2_whenCommand1_thenError1")
  }

  @Test
  fun `test testMethodName with no events in given stage`() {
    // Create a specification with no events in the given stage
    val command = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )
    val resultEvent = Event(
      id = "event3",
      typeReference = EventType(id = "event3", name = "Event3", schema = null),
      value = null
    )

    val emnSpecification = EmnSpecification(
      id = "spec1",
      name = "Test Specification",
      givenStage = EmnGivenStage(
        id = "given1",
        values = listOf(
          // Only non-event elements
          Command(
            id = "nonEvent",
            typeReference = CommandType(id = "nonEvent", name = "SomeElementNotEvent", schema = null),
            value = null
          )
        )
      ),
      whenStage = EmnWhenStage(
        id = "when1",
        values = listOf(command)
      ),
      thenStage = EmnThenStage(
        id = "then1",
        values = listOf(resultEvent)
      )
    )

    // Verify the test method name
    assertThat(Specification(emnSpecification).testMethodName).isEqualTo("givenNoEvents_whenCommand1_thenEvent3")
  }

  @Test
  // @Disabled("Convert to validation test")
  fun `specification requires exactly one command in when stage`() {
    // Create a specification with multiple commands in the when stage
    val command1 = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )
    val command2 = Command(
      id = "command2",
      typeReference = CommandType(id = "command2", name = "Command2", schema = null),
      value = null
    )
    val resultEvent = Event(
      id = "event3",
      typeReference = EventType(id = "event3", name = "Event3", schema = null),
      value = null
    )

    val emnSpecification = EmnSpecification(
      id = "spec1",
      name = "Test Specification",
      givenStage = EmnGivenStage(
        id = "given1",
        values = listOf()
      ),
      whenStage = EmnWhenStage(
        id = "when1",
        values = listOf(command1, command2) // Multiple commands in when stage
      ),
      thenStage = EmnThenStage(
        id = "then1",
        values = listOf(resultEvent)
      )
    )

    // Verify that an IllegalArgumentException is thrown with the appropriate message
    assertThatThrownBy {
      Specification(emnSpecification)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Currently when stage requires exactly one command.")
  }

}
