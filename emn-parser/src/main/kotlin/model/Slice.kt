package io.holixon.emn.model

data class Slice(
    val id: String,
    val name: String? = null,
    val flowElements: List<FlowElement>
) {
  fun commands(): List<Command> {
    return this.flowElements.filterIsInstance<Command>()
  }

  fun events(): List<Event> {
    return this.flowElements.filterIsInstance<Event>()
  }

  fun containsFlowElement(element: FlowElement): Boolean = this.flowElements.contains(element)
}
