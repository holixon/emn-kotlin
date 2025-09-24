@file:OptIn(ExperimentalKotlinPoetApi::class)

package io.holixon.emn.generation.model

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.model.Specification.Stage.*
import io.holixon.emn.generation.simpleName
import io.holixon.emn.model.Command
import io.holixon.emn.model.Event
import io.holixon.emn.model.Slice
import io.konform.validation.Validation
import io.konform.validation.required
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.simpleName
import io.toolisticon.kotlin.generation.poet.KDoc
import io.holixon.emn.model.Error as EmnError
import io.holixon.emn.model.Specification as EmnSpecification
import io.holixon.emn.model.WhenStage as EmnWhenStage

/**
 * Representation of a parsed and validated EMN specification.
 *
 * We make some assumptions and simplifications here:
 * - Given stage may contain zero or more events.
 * - When stage must contain exactly one command.
 * - Then stage may contain either:
 *   - zero events (ThenEmpty)
 *   - one or more events (ThenEvents)
 *   - exactly one error (ThenError)
 */
data class Specification(
  val id: String,
  val name: String,
  val sliceId: String? = null,
  val scenario: String?,
  val givenStage: GivenStage,
  val whenStage: WhenStage,
  val thenStage: ThenStage
) {
  companion object {

    /**
     * Define validation rules for [EmnSpecification] to be valid for transformation to [Specification].
     */
    val validateParsedSpecification = Validation {
      EmnSpecification::givenStage required {
      }
      EmnSpecification::whenStage {
        required {
          EmnWhenStage::commands {
            constrain("Currently when stage requires exactly one command.") {
              it.size == 1
            }
          }
        }
      }
      EmnSpecification::thenStage {
        required {
          dynamic { stage ->
            if (stage.events.isNotEmpty()) {
              constrain("ThenStage must not include events and errors at the same time.") {
                stage.events.isEmpty() || stage.errors.isEmpty()
              }
              constrain("If errors are present, only one error is allowed.") {
                stage.errors.size <= 1
              }
            }
          }
        }
      }
    }

    /**
     * Secondary constructor and validator for [Specification] from [EmnSpecification].
     */
    operator fun invoke(specification: EmnSpecification): Specification {
      require(validateParsedSpecification(specification).isValid) {
        "Specification $specification is not valid: ${validateParsedSpecification(specification).errors}"
      }

      return Specification(
        id = specification.id,
        sliceId = specification.slice?.id,
        name = specification.name,
        scenario = specification.scenario,
        givenStage = GivenStage(specification.givenStage!!.events),
        whenStage = WhenStage(specification.whenStage!!.commands.single()),
        thenStage = if (specification.thenStage!!.values.isEmpty()) {
          ThenStage.ThenEmpty
        } else if (specification.thenStage!!.events.isNotEmpty()) {
          ThenStage.ThenEvents(specification.thenStage!!.events)
        } else {
          ThenStage.ThenError(specification.thenStage!!.errors.single())
        }
      )
    }
  }

  @Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
  sealed interface Stage {
    val funName: String

    data class GivenStage(val events: List<Event>) : Stage, List<Event> by events {

      override val funName = if (isEmpty()) {
        "givenNoEvents"
      } else {
        "given${joinToString("And") { it.simpleName }}"
      }
    }

    data class WhenStage(val command: Command) : Stage {
      override val funName = "when${simpleName(command.typeReference.name)}"
    }

    sealed interface ThenStage : Stage {

      @Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
      data class ThenEvents(val events: List<Event>) : ThenStage, List<Event> by events {

        init {
          require(events.isNotEmpty())
        }

        override val funName: String = "then${joinToString("And") { it.simpleName }}"
      }

      data class ThenError(val error: EmnError) : ThenStage {
        override val funName: String = "then${error.simpleName}"
      }

      data object ThenEmpty : ThenStage {
        override val funName: String = "thenNoEvents"
      }
    }
  }

  @OptIn(ExperimentalKotlinPoetApi::class)
  val messageKdoc by lazy {
    KDoc.of(
      """
        ${"\n" + name}
        ${if (scenario != null) "\nScenario: $scenario" else ""}
      """.trimIndent()
    )
  }

  val testMethodName = listOf(givenStage, whenStage, thenStage).joinToString("_") { it.funName }
}

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
@JvmInline
value class Specifications(private val values: List<Specification>) : List<Specification> by values {
  constructor(vararg references: Specification) : this(references.toList())

  /**
   * Retrieves a list of specifications for given slice.
   * @param slice: slice to look for specifications.
   * @return list of specification referencing given slice.
   */
  operator fun get(slice: Slice): Set<Specification> = get(slice.id)

  /**
   * Retrieves a list of specifications for given slice.
   * @param sliceId: slice id to look for specifications.
   * @return list of specification referencing given slice.
   */
  operator fun get(sliceId: String): Set<Specification> = values.filter { spec -> spec.sliceId != null && spec.sliceId == sliceId }.toSet()
}
