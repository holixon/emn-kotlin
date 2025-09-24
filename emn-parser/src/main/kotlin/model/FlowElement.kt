package io.holixon.emn.model

sealed class FlowElement(
  open val id: String,
  open val typeReference: FlowElementType,
  open val value: ElementValue?,
) {

  override fun toString(): String =
    "${this::class.simpleName}(id=$id, name=${typeReference.name}, type=${typeReference.id}, value=${value})"

}

data class MessageFlow(
  override val id: String,
  override val typeReference: MessageFlowType,
  val source: FlowNode,
  val target: FlowNode
) : FlowElement(id = id, typeReference = typeReference, value = null)

sealed class FlowNode(
  override val id: String,
  override val typeReference: FlowNodeType,
  override val value: ElementValue?,
  open val incoming: MutableList<MessageFlow> = mutableListOf(),
  open val outgoing: MutableList<MessageFlow> = mutableListOf(),
) : FlowElement(id = id, typeReference = typeReference, value = value)

class Command(id: String, typeReference: CommandType, value: ElementValue?) : FlowNode(id = id, typeReference = typeReference, value = value) {

  val commandType: CommandType = typeReference

  fun sourcingEvents(): List<Event> {
    val directEvents = this.views()
      .flatMap { view -> view.queries() }
      .map { query -> query.events() }
      .flatten()
    return directEvents + directEvents
      .filterNot { directEvents.contains(it) }
      .map { event ->
        event.commands().filterNot { it == this }
          .map { it.sourcingEvents() }.flatten()
      }.flatten()
  }

  fun possibleEvents() = this.outgoing.map { flow -> flow.target }.events()
  fun views() = this.incoming.map { flow -> flow.source }.views()
}

class Query(id: String, typeReference: QueryType, value: ElementValue?) :
  FlowNode(id = id, typeReference = typeReference, value = value) {
  fun events() = this.incoming.map { flow -> flow.source }.events()
  val queryType: QueryType = typeReference
}

class Event(id: String, typeReference: EventType, value: ElementValue?) :
  FlowNode(id = id, typeReference = typeReference, value = value) {
  fun commands() = this.incoming.map { flow -> flow.source }.commands()
  val eventType: EventType = typeReference
}

class ExternalEvent(id: String, typeReference: ExternalEventType, value: ElementValue?) :
  FlowNode(id = id, typeReference = typeReference, value = value)

class View(id: String, typeReference: ViewType, value: ElementValue?) :
  FlowNode(id = id, typeReference = typeReference, value = value) {
  fun queries() = this.incoming.map { flow -> flow.source }.queries()
}

class Translation(id: String, typeReference: TranslationType, value: ElementValue?) :
  FlowNode(id = id, typeReference = typeReference, value = value)

class Automation(id: String, typeReference: AutomationType, value: ElementValue?) :
  FlowNode(id = id, typeReference = typeReference, value = value)

class ExternalSystem(id: String, typeReference: ExternalSystemType, value: ElementValue?) :
  FlowNode(id = id, typeReference = typeReference, value = value)

class Error(id: String, typeReference: ErrorType, value: ElementValue?) :
  FlowNode(id = id, typeReference = typeReference, value = value) {
  val errorType: ErrorType = typeReference
}

class FlowNodeReference(id: String) : FlowNode(id = id, typeReference = NoTypeReference, value = null)
