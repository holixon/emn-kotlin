# quizmania-json-schema

This example generates Kotlin types from the shared and embedded JSON Schemas in the Quizmania EMN blueprint.

Run the Maven example with:

```bash
./mvnw -pl examples/quizmania-json-schema -am test
```

After installing the plugin artifacts in Maven Local, run the same example with Gradle:

```bash
gradle -p examples :quizmania-json-schema:build
```

The generated Kotlin sources are written to:

```text
examples/quizmania-json-schema/target/generated-sources/emn-schema-kotlin # Maven
examples/quizmania-json-schema/build/generated/sources/emn-schema-kotlin # Gradle
```

The source diagram is:

```text
_emn/quizmania/quizmania-blueprint.emn
```

The configured root package is `io.holixon.emn.example.quizmania`. The generator applies its
opinionated package split: commands use `.command`, events use `.event`, queries use `.query`, and
shared schema definitions use `.common`.

The generator uses the EMN TypeId as the Kotlin declaration name. Shared object schemas,
enums, composed schemas, commands, events, and queries are generated as Kotlin declarations.
Named scalar schemas are generated as Kotlin type aliases, so the UUID-based ID definitions and
the `int32` round-number definitions retain their domain names in referenced properties. A
dictionary-only object with `propertyNames` and schema-valued `additionalProperties` is also
generated as a type alias; for example, `GamePlayerScores` becomes
`Map<GamePlayerId, Int>`.

`EvaluateBuzzesCommand` composes the shared `GameCommand` schema with JSON Schema `allOf`.
The generator emits this as Kotlin inheritance. The neutral `x-emn-role: "entity-id"` hint on
`GameCommand.gameId` is converted to Axon's `@TargetEntityId` because this example enables the
`axon5` annotation enhancer.

All domain commands compose `GameCommand`, and all domain events compose `GameEvent`.
Question events use a two-level hierarchy by composing `GameQuestionEvent`, which itself composes
`GameEvent`. The shared event ID properties carry neutral `x-emn-tag-key` hints that the Axon 5
enhancer converts to `@EventTag`.

The example also enables the `jackson` annotation enhancer. The common `Question` object schema
uses `discriminator.propertyName` and an explicit `discriminator.mapping`; concrete schemas extend
it through `allOf`. The enhancer validates each mapped subtype's discriminator `const` and adds
Jackson's `@JsonTypeInfo` and `@JsonSubTypes` annotations to the generated base class.
This allows a JSON array of `Question` values to be deserialized into the generated concrete
question classes.
