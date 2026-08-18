# Contributing

Thanks for your interest in this project. It is currently maintained by a single maintainer;
the notes below keep contributions predictable for both sides.

## Building

```bash
mvn -B verify
```

That runs the unit tests plus the integration test, which generates code against a sample
project and byte-compares it with the committed golden corpus
(`jpa-metadata-maven-plugin/src/it/simple-consumer/expected/`). CI builds on JDK 17, 21 and 25;
the source level is Java 17.

Two things reviewers will hold your PR to:

- **Golden corpus changes are deliberate.** Any diff under `src/it/simple-consumer/expected/`
  changes the plugin's output contract and needs an explicit rationale in the PR description.
- **Reproducible output.** Generated code must stay byte-deterministic — no timestamps, no
  environment-dependent content. `GenerationReproducibilityTest` enforces this.

## Filing issues

Use the issue templates:

- **Bug report** — include plugin version, JDK, Spring Data versions and a minimal entity sample.
- **Use case** — tell us what you are building and what would make this tool work for you. These
  reports directly shape the roadmap.

Initial response target for new issues and reports: within 14 days.

## Pull requests

- PRs target `master`. Keep them small and single-topic.
- `mvn -B verify` must pass locally before you open the PR.
- New behavior needs a test; changed generator output needs the corpus rationale above.

## Licensing

This project is licensed under Apache-2.0. Contributions are accepted under the same license
(inbound = outbound). There is no CLA and no DCO requirement.

## Direction

See [ROADMAP.md](ROADMAP.md) for where the project is heading before proposing large changes —
a major rework of the generation pipeline is in design, and PRs against surfaces it replaces
may be declined with a pointer to the roadmap.
