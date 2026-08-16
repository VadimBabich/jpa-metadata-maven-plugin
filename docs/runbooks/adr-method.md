# Runbook — The ADR Method

## Status

`_doc/adr/` holds **twelve ADRs — one per open question in HLD §8 — all Proposed, none
ratified.** They are private working material (gitignored with the rest of `_doc/`), so
they are cited in commit messages and discussions **by filename**, never linked from
committed files as if a reader could open them.

Each ADR follows the shape below and records its own adversarial review in a
`**Revision:**` line. Ratification happens at the decision point each ADR names (the
review unit, a specific workstream, or a gate) — until then their decisions are
proposals, and committed artifacts must not assume them.

## 1. Frame

- Locate the question's owner and constraints in the HLD (§5 decision log, §8 register).
- State the scope, and **declare any frame revision explicitly**: if the evidence shows
  the question is mis-posed (a "decided" default that the facts defeat, an absolute rule
  the design already violates, an option list missing the real answer), say so in the
  Scope line. Never silently answer a different question than the one asked.

## 2. Research — primary sources only

- Specs, source at pinned commits/tags, published artifacts (POMs, jars, Central
  metadata), issue trackers, official docs. Blogs and forum posts are supporting colour,
  labelled as such.
- Every claim carries a source URL and a verbatim quote (or exact code/metadata excerpt).
- Mark unverifiable items UNVERIFIED. Distinguish spec-guaranteed from de-facto behaviour
  from inference — and label inferences as inferences.
- Measure local claims yourself (grep, count, hash) instead of repeating numbers from
  earlier documents; several long-standing figures turned out to be raw grep counts.
- Negative results are results: "no such policy exists" and "the tracker has no such
  issue" are findings worth recording, with the search coverage stated.

## 3. Draft

Standard shape: Status · Date · Scope (with declared frame revisions) · Method ·
findings with inline citations · Decision (numbered) · Consequences (including which
other documents must change, and any demand-gated register entry with an owner and a
written trigger) · prior-art table · Key sources with verification levels.

## 4. Adversarial review — then verify the review

- Have the draft read hostilely: every quote re-checked, every number re-derived, every
  inference challenged.
- **Re-verify each review finding against primary sources before applying it.** Reviews
  are wrong at a measurable rate — six findings across the first twelve ADRs were refuted
  on re-check (misquoted javadoc, a "missing" quote that was verbatim on the live page, a
  wrong-source attribution). Apply what verifies, refute what does not, and record both
  outcomes in the ADR's `**Revision:**` line so the next reviewer does not re-litigate
  settled ground.
- Findings that hold are usually about scope, not facts: an overclaim, an undisclosed
  residual risk, a promise that belongs to the substrate rather than to us.

## 5. Close

- Update cross-references and the memory index.
- Consequences that touch the HLD are **flagged for its next revision, never silently
  edited into it** — the HLD is the ratified record; ADRs propose against it.
- Files stay uncommitted unless the owner asks; ADRs live in `_doc/adr/` permanently.
