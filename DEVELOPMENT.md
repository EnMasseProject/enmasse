# Development Guidelines

This document describes the development guidelines when writing and maintaining EnMasse source code. 

## General considerations

As EnMasse is a polyglot project with multiple languages in the same repo, it uses `make` as a
single entry point for invoking a full project build. Make will invoke the language-specific build 
single entry point for invoking a full project build. Make will invoke the language-specific build
system.

### Introducing new dependencies

If you need a function or class utility that requires adding a new dependency, first consider:

* If the dependency has a reliable release schedule and track record for security updates.
* If the dependency pulls in transitive dependencies that increase the footprint significantly
* If you can copy the function or class (assuming the license allows it) into the EnMasse source tree. This should only be done for trivial amounts of code where the maintenance cost is expected to be low.
* If the cost of implementing and maintaining the functionality yourself is worthwhile in the same manner as above.

## Java

Java source code is built using Maven.

Source code should follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with the following amendments:

* Use 4 spaces indentation
* Newlines should be used between annotations and function/class definitions

Prefer to use plain Java whenever you can. Code should be easy to understand without intimate knowledge of specific frameworks. VertX, Jackson, JAX-RS and tests framework annotations such as Junit are in use at present, and should be preferred instead of introducing other frameworks. However, some dependencies like Eclipse Hono uses Spring, in which case it cannot be ignored.

## Go

Golang dependencies are managed using glide (the project will move to go modules once dependencies have).

Use `gofmt` to format all the source code.

## JavaScript

JavaScript dependencies are managed using `npm`, but it is invoked using maven and the
`frontend-maven-plugin`.

JavaScript code generally follows [JavaScript Style Guide and Coding Conventions](https://www.w3schools.com/js/js_conventions.asp) with the most important being:

* 2 spaces indentation
* Variable names and functions use camelcase
