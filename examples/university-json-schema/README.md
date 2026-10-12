# university-json-schema

This example generates Kotlin data classes and Axon fixture tests from embedded JSON Schemas
and Given/When/Then specifications in an EMN file.
It also enables the `axon5` annotation enhancer, which maps neutral JSON Schema hints to Axon annotations:

- `x-emn-role: "entity-id"` -> `@TargetEntityId`
- `x-emn-tag-key: "course"` -> `@EventTag(key = "course")`

The Maven build copies the repository-level `_emn` folder into generated resources and runs
both `generate-schema-kotlin` and `generate-specification-tests`:

```bash
./mvnw -pl examples/university-json-schema -am generate-sources
```

After installing the plugin artifacts in Maven Local, the same example can be built with Gradle:

```bash
gradle -p examples :university-json-schema:build
```

The generated classes and specification tests are written to:

```text
examples/university-json-schema/target/generated-sources/emn-schema-kotlin # Maven
examples/university-json-schema/target/generated-test-sources/emn-specification-tests # Maven
examples/university-json-schema/build/generated/sources/emn-schema-kotlin # Gradle
examples/university-json-schema/build/generated/sources/emn-specification-tests # Gradle
```

The configured root package is `io.holixon.emn.example.university`. Commands are generated in
`.command`, events in `.event`, queries in `.query`, and common schema definitions in `.common`.

The example generates command, event, query, error, and common schema types, plus tests for
the create-course and rename-course specifications from:

```text
_emn/faculty-json-schema/faculty.emn
```

The generated methods carry `@EmnSpecification` with the corresponding EMN specification ID.
The generated classes use Axon's `AxonFrameworkExtension` and the `UniversityFixtureProvider`
from `src/test/kotlin/io/holixon/emn/example/university/SpecificationFixture.kt`. No custom JUnit
extension registration or `META-INF` service file is needed.
