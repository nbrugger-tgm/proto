## Proto

Annotation **Pro**cessor **To**ols

![testcoverage](https://img.shields.io/badge/coverage-95%2C4%25-green)

### About

This is a collection of classes that helps with writing Java Annotation Processors. There are 3 min points this project provides

- Verification of annotated elements
- Logging at compile time
- A bugfix for a bug in the Java compiler AP API [[JDK-8256826] Source generated in last round not visible to other sources - Java Bug System](https://bugs.openjdk.java.net/browse/JDK-8256826)

### Usage

When writing an annotation processor the base class to use is ``BaseProcessor``.

When doing so you will have two fields

- logger
- verifier

that provide high level access to the java processing API.

The bugfix mentioned above will automatically be applied to your project if you don' t disable it manually.

To generate classes it is highly recommended to use JavaPoet (included,[Introduction to JavaPoet | Baeldung](https://www.baeldung.com/java-poet)) and the ``writeClass()`` method.

### Examples

```java
verifier.isClass(someElement)
    .because("only classes can be mapped")
    .failOnViolation();
```

> **java: error** someElement should be a class, because only classes can be mapped

```java
verifier.isAnnotated(someElement, Override.class)
    .because("only overriden methods can be compile time mocked")
    .failOnViolation();
```

> **java: error** someElement should be annotated with @Override, because only overriden methods can be compile time mocked

```java
verifier.doesExtend(myElement, Serializable.class)
    .not()
    .because("Serializable is not state of the art")
    .warnOnViolation();
```

> **java: warning** myElement should not extend Serializable, because Serializable is not state of the art
