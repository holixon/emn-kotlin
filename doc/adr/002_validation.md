# ADR-002: Validation Strategy

The emn specification must be flexible, with many elements and properties being optional, so we can have a
spec-compliant model as graphical representation to discuss business requirements, even if not all details are known.

However, for a model to be useful in a production environment, it must be complete and valid according to the specification,
so we can use the definitions for code generation without continuously wondering whether all required elements are defined.

For the relation between the emn-parser and the emn-generator, this means that the parser must be able to parse incomplete models,
but the generator must only accept complete and valid models.

So we parse any emn-compliant model, but we validate the effective context before passing it to the generator. 

The validation is done using [konform.io](https://www.konform.io/), because it proved to be a very flexible and powerful validation library for Kotlin.
It allows to define complex validation rules, including cross-field validations, and it provides detailed error messages and does not require
any additional dependencies (such as hibernate-validation) and thus is a good option for usage in a maven code generation scenario
where we do not want to set up a complex [JSR-330](https://beanvalidation.org/1.0/spec/) bean validation scenario.
