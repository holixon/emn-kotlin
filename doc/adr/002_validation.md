# ADR-002: Validation Strategy

The EMN specification must be flexible, with many elements and properties being optional, so we can have a
spec-compliant model as graphical representation to discuss business requirements, even if not all details are known.

However, for a model to be useful in a code generation environment, it must be not only complete and valid according 
to the specification but also comply to additional semantic constraints. By checking these constraints, we can use 
the definitions for code generation without continuously wondering whether all required elements are defined.

For the relation between the emn-parser and the emn-generator, this means that the parser must be able to parse models
all EMN models, but the generator must only accept complete models. More specifically, the model completeness is not 
a global property, but must be seen on level of generator cartridge. For example, an EMN document without a specification
can be used by one cartridge to generate messaging components. But the cartridge for generation of Given/When/Then tests require
specifications to operate.

So we parse any emn-compliant model, but we validate the effective context before passing it to the generator cartridges. 

The validation is done using [konform.io](https://www.konform.io/), because it proved to be a very flexible and powerful validation library for Kotlin.
It allows to define complex validation rules, including cross-field validations, and it provides detailed error messages and does not require
any additional dependencies (such as hibernate-validation) and thus is a good option for usage in a maven code generation scenario
where we do not want to set up a complex [JSR-330](https://beanvalidation.org/1.0/spec/) bean validation scenario.
