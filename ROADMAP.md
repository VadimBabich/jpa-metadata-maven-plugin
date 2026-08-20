# Roadmap

As of 2026-08-18. Updated when direction changes, reviewed at least once per release cycle.

## Where the project is

The shipping artifact is the 1.x Maven plugin: it parses Spring Data Relational entity sources
and generates static metamodel classes for type-safe query construction with Spring Data R2DBC.
Version 1.0.0 is abandoned; the 1.1.x line is current.

## Where it is going

A **2.0 reboot** is in design and early implementation, replacing the source-parsing Maven
plugin with a **JSR-269 annotation processor** plus a small owned runtime library:

- **Runtime library** — typed references (`EntityRef` / `PropertyRef` / `JoinRef`) that generated
  metamodels compile against, with no framework types in the public API surface.
- **Annotation processor** — build-tool-neutral generation (Maven, Gradle, IDE builds),
  incremental-compilation aware.
- **Fluent query surface** — typed, composable `SELECT` construction over Spring Data R2DBC,
  including joins, driven by the generated metamodel.
- **BOM** — one aligned version for the whole family.

The 2.0 line ships as milestones (`2.0.0-M1` → RC → GA). The 1.x plugin is maintained through
the transition and retired in stages after the processor reaches output parity — the generated
code of both generations is held byte-identical by a committed golden corpus until then.

## What guides the order

Correctness gates before features: output-shape freeze, incremental-compilation verification,
and API/SPI compatibility gating all precede the first 2.0 publication to Maven Central.

## Influencing the roadmap

File a **use case** issue (template provided). Real usage reports directly reprioritize this
list — that is not a platitude; the fluent-query design was derived from measured usage in real
codebases.
