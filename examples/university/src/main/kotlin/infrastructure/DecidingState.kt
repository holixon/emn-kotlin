package io.holixon.emn.example.university.infrastructure

/**
 * State which can decide.
 */
interface DecidingState<COMMAND : Any> {

  fun decide(command: COMMAND): List<Any>
}
