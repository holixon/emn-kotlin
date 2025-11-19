package io.holixon.emn

import io.holixon.emn.model.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.StringReader
import org.dom4j.io.SAXReader

class ParserSemanticTest {

    private val parser = EmnDocumentParser()

    @Test
    fun `typeRefs of elements must be resolved correctly`() {
        // Create a simple EMN document with a type reference that doesn't exist
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
                <emn:types>
                    <!-- No types defined -->
                </emn:types>
                <emn:timeline id="timeline-1">
                    <emn:command id="command-1" typeRef="non-existent-type" />
                </emn:timeline>
            </emn:definitions>
        """.trimIndent()

        val reader = SAXReader()
        val document = reader.read(StringReader(xml))

        // The parser should throw an exception when it can't resolve a type reference
        assertThatThrownBy {
            parser.parseDefinitions(document)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Required value was null")
    }

    @Test
    fun `ids of referenced elements must be present on type level`() {
        // Create a simple EMN document with a flow that references non-existent types
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
                <emn:types>
                    <emn:informationFlowType id="flow-type-1" sourceRef="non-existent-source" targetRef="non-existent-target" />
                </emn:types>
                <emn:timeline id="timeline-1">
                    <!-- No elements defined -->
                </emn:timeline>
            </emn:definitions>
        """.trimIndent()

        val reader = SAXReader()
        val document = reader.read(StringReader(xml))

        // The parser should throw an exception when it can't resolve a type reference
        assertThatThrownBy {
            parser.parseDefinitions(document)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown source non-existent-source")
    }

    @Test
    fun `ids of referenced elements must be present on element level`() {
        // Create a simple EMN document with a flow that references non-existent elements
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
                <emn:types>
                    <emn:commandType id="command-type-1" />
                    <emn:eventType id="event-type-1" />
                    <emn:informationFlowType id="flow-type-1" sourceRef="command-type-1" targetRef="event-type-1" />
                </emn:types>
                <emn:timeline id="timeline-1">
                    <emn:informationFlow id="flow-1" typeRef="flow-type-1" sourceRef="non-existent-source" targetRef="non-existent-target" />
                </emn:timeline>
            </emn:definitions>
        """.trimIndent()

        val reader = SAXReader()
        val document = reader.read(StringReader(xml))

        // The parser should throw an exception when it can't resolve an element reference
        assertThatThrownBy {
            parser.parseDefinitions(document)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown source non-existent-source")
    }

    @Test
    fun `types must match to elements`() {
        // Create a simple EMN document with mismatched types and elements
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
                <emn:types>
                    <emn:commandType id="command-type-1" />
                    <emn:eventType id="event-type-1" />
                </emn:types>
                <emn:timeline id="timeline-1">
                    <!-- Using event-type-1 for a command element -->
                    <emn:command id="command-1" typeRef="event-type-1" />
                </emn:timeline>
            </emn:definitions>
        """.trimIndent()

        val reader = SAXReader()
        val document = reader.read(StringReader(xml))

        // The parser should throw an exception when the type doesn't match the element
        assertThatThrownBy {
            parser.parseDefinitions(document)
        }.isInstanceOf(ClassCastException::class.java)
            .hasMessageContaining("cannot be cast to class io.holixon.emn.model.CommandType")
    }

    @Test
    fun `lane references must be valid`() {
        // Create a simple EMN document with invalid lane references
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
                <emn:types>
                    <emn:commandType id="command-type-1" />
                </emn:types>
                <emn:timeline id="timeline-1">
                    <emn:laneSet>
                        <emn:triggerLaneSet>
                            <emn:triggerLane id="trigger-lane-1">
                                <emn:flowNodeRef>non-existent-node</emn:flowNodeRef>
                            </emn:triggerLane>
                        </emn:triggerLaneSet>
                        <emn:interactionLane id="interaction-lane-1" />
                        <emn:conceptLaneSet>
                            <emn:conceptLane id="concept-lane-1" />
                        </emn:conceptLaneSet>
                    </emn:laneSet>
                    <emn:command id="command-1" typeRef="command-type-1" />
                </emn:timeline>
            </emn:definitions>
        """.trimIndent()

        val reader = SAXReader()
        val document = reader.read(StringReader(xml))

        // The parser should throw an exception when it can't resolve a lane reference
        assertThatThrownBy {
            parser.parseDefinitions(document)
        }.isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("Key non-existent-node is missing in the map")
    }

    @Test
    fun `slice references must be valid`() {
        // Create a simple EMN document with invalid slice references
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
                <emn:types>
                    <emn:commandType id="command-type-1" />
                </emn:types>
                <emn:timeline id="timeline-1">
                    <emn:sliceSet>
                        <emn:slice id="slice-1">
                            <emn:flowNodeRef>non-existent-node</emn:flowNodeRef>
                        </emn:slice>
                    </emn:sliceSet>
                    <emn:command id="command-1" typeRef="command-type-1" />
                </emn:timeline>
            </emn:definitions>
        """.trimIndent()

        val reader = SAXReader()
        val document = reader.read(StringReader(xml))

        // The parser should throw an exception when it can't resolve a slice reference
        assertThatThrownBy {
            parser.parseDefinitions(document)
        }.isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("Key non-existent-node is missing in the map")
    }
}
