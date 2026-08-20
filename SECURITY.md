# Security Policy

## Reporting a vulnerability

Please report suspected vulnerabilities privately via GitHub's security advisories:
**[Report a vulnerability](../../security/advisories/new)** (Security tab → Report a
vulnerability). Do not open a public issue for a suspected vulnerability.

You can expect an initial response within **14 days**.

## Supported versions

Honest status, so expectations are right:

| Version | Status |
|---|---|
| 1.0.0 | Abandoned — do not use; it will not receive fixes. |
| 1.1.x | Current line — fixes land here until the 2.0 reboot ships. |
| 2.0.0 (planned) | In design; a reboot of the generation pipeline. Not released. |

## Scope notes

The plugin runs at build time and parses Java source files; it performs no network I/O and
executes none of the code it parses. Reports about the build-time parsing path (e.g. crafted
source files causing pathological behavior) are in scope.
