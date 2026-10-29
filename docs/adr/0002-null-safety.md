# 0002. Null safety checked at compile time

- Status: accepted
- Date: 2026-10-29

## Context

`NullPointerException` is the most common Java crash. In payment code, a
missing value must fail loudly at build time, not as a half-finished transfer
in production. Java's type system doesn't say whether a value can be `null`.

## Options

**A. Discipline and code review.** Check for `null` where it seems needed.
Free, but it depends on people never forgetting.

**B. `Optional` everywhere.** Return `Optional<T>` when a value may be absent.
Good for return values, but awkward for fields and parameters, and it doesn't
stop a plain `null` from being passed anywhere.

**C. Null annotations with a checker.** Mark what can be null and let a tool
prove the rest never is. Tools: the IntelliJ inspection (editor only, not in
the build), the Checker Framework (thorough but slow and verbose), or
**NullAway** (fast, built for large codebases at Uber, runs inside `javac`).

## Decision

**C: [JSpecify](https://jspecify.dev) annotations checked by
[NullAway](https://github.com/uber/NullAway) on every build.**

JSpecify is the standard that Spring Framework 7 itself uses, so Spring's own
APIs already say what can be null. NullAway runs as an
[Error Prone](https://errorprone.info) plug-in, so the normal `./mvnw verify`
fails on a possible null mistake, the same as a type error. We also use
`Optional` for return values where "not found" is a normal outcome.

## How it works in the code

- Every package has a `package-info.java` with `@NullMarked`: all types in it are
  non-null unless written `@Nullable`.
- `@NullMarked` does not apply to sub-packages, and NullAway only checks marked
  packages (`OnlyNullMarked=true` in `pom.xml`). So `NullSafetyTests` fails the
  build when a production package has no null-marked `package-info.java`.
  Together they guarantee all production code is checked.
- The compiler also runs with `-Xlint:all -Werror`: every compiler warning is an
  error.
