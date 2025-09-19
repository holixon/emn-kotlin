package io.holixon.emn.model

fun List<FlowElement>.commands() = filterIsInstance<Command>()
fun List<FlowElement>.events() = filterIsInstance<Event>()
fun List<FlowElement>.errors() = filterIsInstance<Error>()
fun List<FlowElement>.views() = filterIsInstance<View>()
fun List<FlowElement>.queries() = filterIsInstance<Query>()

inline fun <reified T : Any> List<AggregateLane>.applyIfExactlyOne(
  noneFoundMessageSupplier: () -> Unit ,
  multipleFoundSupplier: () -> Unit,
  transformation: (AggregateLane) -> T,
): T? {
  return when (this.size) {
    0 -> {
      noneFoundMessageSupplier()
      null
    }

    1 -> transformation.invoke(this[0])
    else -> {
      multipleFoundSupplier()
      null
    }
  }
}
