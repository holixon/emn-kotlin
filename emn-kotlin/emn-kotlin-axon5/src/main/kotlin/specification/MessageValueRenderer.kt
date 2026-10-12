package io.holixon.emn.generation.specification

import com.squareup.kotlinpoet.CodeBlock
import io.holixon.emn.model.FlowNode

/** Schema-source-neutral boundary for turning an EMN example value into Kotlin code. */
fun interface MessageValueRenderer {
  fun render(message: FlowNode): CodeBlock
}
