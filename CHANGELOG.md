# Changelog

## 1.1.0 (unreleased)

Consumer-visible changes:

- **`outputDirectory` default changed** from `${project.build.outputDirectory}` (`target/classes`)
  to `${project.build.directory}/generated-sources/metamodel`. Generated `.java` files are no
  longer packaged into the consumer's jar. Builds that referenced generated sources under
  `target/classes` must switch to the new location or set `outputDirectory` explicitly.
- **Java baseline lowered to 17** (was 21 bytecode). The plugin now runs on JDK 17 Maven builds.
- **Generated output is deterministic and reproducible**: constants are emitted in declaration
  order (previously unspecified, varying between runs), and file headers no longer embed the
  generation date. Regenerating over unchanged sources is byte-identical.
- **Generated file headers** now name the actual plugin (`jpa-metadata-maven-plugin` instead of
  the stale `entity-metadata-plugin`).

Internal/build changes: PR and multi-JDK (17/21/25) CI, manual-dispatch-only release workflow,
integration test compiling generated sources against a committed golden corpus, LICENSE file
added, dependency cleanup (`reflections` and `maven-project` removed).

## 1.0.0 — 2025-06-19

Initial release (GitHub Packages only).
