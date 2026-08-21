# Shell Code Style

Rules for every shell script in this repository: the guards under `.github/scripts/`, the hooks
under `.githooks/`, and the inline `run:` blocks in `.github/workflows/`. The counterpart for Java
is `CLAUDE.md` § Code style; for reactive code, `docs/reactive-code-style.md`.

**Core principle.** Optimise for human readability and operational safety, not cleverness. These
scripts run unattended and are read while something is broken. Predictable and explicit beats short.

---

## 1. Prologue and interpreter

- **`#!/usr/bin/env bash` and `set -euo pipefail`.** Fail fast, fail loudly, and never let a
  pipeline hide a failing stage.
- **Do not add `-E`.** This is a deliberate departure from the usual `set -Eeuo pipefail` advice,
  and it is load-bearing: `-E` propagates the `ERR` trap into command-substitution subshells, where
  a trap body ending in `exit 1` **overwrites the subshell's real exit status**. In this repository
  that masked `grep`'s exit 2 (invalid pattern) as exit 1 (no match), which silently disarmed a
  fail-closed branch while printing `ERROR` on nearly every healthy run. Verified by truth table:
  with `-E`, a broken pattern and a clean no-match are indistinguishable; without it, they are 2 and
  1. If you reintroduce `-E`, you must also prove every `$( )` still reports the status its caller
  branches on.
- **Bash, but bash 3.2.** The hooks run on maintainer macOS, which ships bash 3.2.57. Arrays,
  `[[ ]]`, `<<<` and `+=` are fine. **`${var,,}`, `mapfile`/`readarray` and associative arrays are
  not** — use `tr '[:upper:]' '[:lower:]'` and a `while read` loop instead.
- **`readonly` for constants**, declared at the top, before any function.
- **Quote every expansion** — `"${var}"`, never `$var`. No reliance on implicit globbing or word
  splitting. No `eval`, ever.

## 2. Exit status is the thing that goes wrong

Four distinct ways a script here has reported success while doing nothing. Read these before
touching a pipeline.

- **A pipeline reports only its last stage.** `value="$(git log … | sort -u)"` yields `sort`'s
  status, so a failing `git` looks like empty output. Capture first, then transform:

  ```bash
  if ! log_output="$(git log --format='%ae' "${range}")"; then
    log_error "reading authorship for '${range}' failed"
    return 1
  fi

  authors="$(printf '%s\n' "${log_output}" | sort -u)"
  ```

- **A failing command substitution in an *argument* does not trip `set -e`.**
  `scan "label" "$(extract …)"` discards `extract`'s status entirely. Assign to a checked local,
  then pass it.
- **`while read` at the end of a pipeline runs in a subshell**, so anything it accumulates —
  counters, arrays, violation lists — is discarded when that subshell exits. Pass the data as an
  argument and feed the loop with a here-string or here-document instead.
- **Distinguish "nothing found" from "the tool broke".** `grep` exits 1 on no match and ≥2 on
  error. Tolerate 1 explicitly, fail on the rest; never blanket the whole pipeline with `|| true`:

  ```bash
  matches="$(printf '%s\n' "${text}" | grep -oE "${EMAIL_PATTERN}")" || grep_status=$?

  if [[ "${grep_status}" -gt 1 ]]; then
    log_error "address extraction failed (grep exit ${grep_status})"
    return 1
  fi
  ```

**Fail closed on empty or unresolvable input.** A check that examined zero items is not a passing
check. Validate that the input exists *and* is non-empty before reporting success — a guard that
passes because it looked at nothing is worse than no guard, because it is believed.

## 3. Traps

- Use a trap when there is genuinely something to clean up or report. Do **not** add one reflexively:
  without `-E` a trap covers only top-level commands, so if the sole top-level command is
  `main "$@"`, and `main` already reports its own failures, the trap can only ever emit a misleading
  second line.
- **Invoke `main` so handled failures do not trip the trap:**

  ```bash
  if ! main "$@"; then
    exit 1
  fi
  ```

- **No signal handling, no graceful-shutdown machinery.** Every script here is a short-lived guard,
  hook or CI step — not a container entrypoint or PID 1. `SIGTERM` handling, drain loops and
  shutdown hooks are out of scope; adding them is noise. Revisit only if a long-running script
  actually appears.

## 4. Structure

- Group in this order: **prologue → constants → logging → helpers → orchestration → `main`**, with
  blank lines between conceptual steps.
- One responsibility per function. `local` for every variable inside one. No hidden side effects,
  and no flag parameters selecting between behaviours — write two functions.
- **Prefer early returns over nesting.** A candidate that does not apply returns early; it does not
  wrap the remainder in an `else`.
- **When a helper's non-zero status means "not applicable" rather than "failed", print nothing and
  return 0** — let the caller test for empty output. Exit status is for failure, not for signalling
  which branch was taken; conflating the two is what makes trap noise and false errors.
- Extract non-trivial logic out of YAML into a script under `.github/scripts/`. Inline `run:` blocks
  cannot be tested locally, and untestable logic is where bugs survive review.

## 5. Control flow

Explicit, always:

```bash
if ! check_service; then
  log_error "service unavailable"
  exit 1
fi

process_data
```

Never `check_service && process_data || exit 1`. **No `&&`/`||` as a substitute for control flow**,
no inline conditionals inside complex commands, no chained ternary-style tricks. `||` is acceptable
only for capturing a status (`… || status=$?`) or a documented tolerated failure.

**`|| true` requires a stated reason** at the call site, in the code or in a comment explaining what
failure is being tolerated and why it is safe. Blanket-silencing a whole pipeline is a defect.

## 6. Naming

Names carry intent, never type: `retry_interval`, `deadline_epoch`, `config_path`, `tracked_files`,
`authorship_lines`. Never `data`, `tmp`, `var`, `result`, `out`, `x`. A `_raw` suffix usually means
the name is describing a stage rather than a value — name what it holds instead.

## 7. Logging

- One helper per level — `log_info`, `log_warn`, `log_error` — with a consistent format and a UTC
  timestamp. `log_warn` and `log_error` write to **stderr**; only real results go to stdout, so a
  script whose stdout is consumed by a caller stays parseable.
- **Say when coverage was reduced.** A step that silently narrows what it examined — falling back to
  a single commit, skipping a stage, sampling — must log a `WARN` naming what went unexamined.
  Silent narrowing reads as full coverage in a green log.
- **Never log secrets.** No tokens, no keys, no passphrases, not even redacted-looking fragments.
  Prefer clipboard or environment for credential transport; never write them to a file.
- Error messages carry context: what was being attempted, on what input, and what to do about it.

## 8. Tests and the quality gate

- **`shellcheck -S style` must pass** on every script before commit.
- **Assert stderr, not just exit codes.** A guard that exits 0 while printing `ERROR` is broken, and
  a suite that captures stderr into a variable it never inspects will score that as a pass — that
  exact blind spot hid a disarmed fail-closed branch here for two review rounds.
- **Test both directions.** A detection script needs a positive control (it catches the bad thing)
  *and* a negative control (it does not flag the good thing), plus the fail-closed paths: empty
  input, unresolvable input, missing arguments.
- **Test the change that introduces the script.** A guard must permit its own introducing commit;
  if it rejects the documented way to satisfy it, people will bypass it and it protects nothing.
- Exercise scripts in a scratch repository under the scratchpad directory when the test needs real
  commits. Never leave test artefacts, credentials or temporary files in the working tree.

## 9. Idempotency and footprint

Scripts here are read-only inspectors and must stay safe to re-run: no mutation of the working tree,
no writes outside `$RUNNER_TEMP` or a scratch directory, no leftover files. If a script ever needs to
mutate state, it must be re-runnable to the same end state, and say in its header why that holds.
