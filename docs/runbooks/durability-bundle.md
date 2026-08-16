# Runbook — Durability (R-P0)

Run after any significant working session.

## What is durable today, and what is not

| Asset | Where it lives | Durable? |
|---|---|---|
| Code, tests, golden corpus, CI, these runbooks | git, pushed to `origin` (`master` tracks `origin/master`) | **Yes** — on the remote |
| Unpushed feature branches | local git only | Only via the bundle below |
| `_doc/` — HLD, `_doc/adr/` (12 ADRs), investigations, plans, evaluations, diagrams, translations, evidence corpus (~156 files, ~20 MB) | working tree only; **gitignored, so not in git at all** | **No — this is the R-P0 risk** |

**The critical point: `git bundle` archives committed history only. `_doc/` is gitignored,
so no bundle will ever contain it.** The program paper — the design work, the twelve ADRs,
the evidence — survives solely as files on one machine until step 3 below runs.

## 1. Commit and push what belongs in git

```bash
git status --short                      # _doc/ will NOT appear; that is expected
git add <explicit paths>                # never git add .
git commit -m "..."
git push -u origin "$(git branch --show-current)"
```

## 2. Bundle the git history (covers unpushed branches too)

```bash
mkdir -p ~/backups/jpa-metadata
D=$(date +%Y%m%d)
git bundle create ~/backups/jpa-metadata/repo-$D.bundle --all
git bundle verify ~/backups/jpa-metadata/repo-$D.bundle    # expect "records a complete history"
```

## 3. Archive `_doc/` — the whole thing, not just the corpus (PRIVATE location only)

```bash
tar -czf ~/backups/jpa-metadata/doc-$D.tar.gz _doc
# verify the archive really holds the paper, the ADRs and the corpus:
tar -tzf ~/backups/jpa-metadata/doc-$D.tar.gz | grep -c '^_doc/adr/adr-.*\.md$'   # expect 12
tar -tzf ~/backups/jpa-metadata/doc-$D.tar.gz | grep -c 'hld-entity-metamodel.md' # expect >= 1
tar -tzf ~/backups/jpa-metadata/doc-$D.tar.gz | grep -c '^_doc/repo_usage/'       # expect 28
```

This archive contains another party's proprietary source (the evidence corpus) and
verbatim production internals throughout the paper. It must never be placed anywhere
public, org-shared, or rights-unclear. Encrypt it if the destination is cloud storage.

## 4. Off-machine copy

Copy **both** files (`repo-$D.bundle`, `doc-$D.tar.gz`) to at least one location that does
not share this machine's fate. Two files, one date, always together — the bundle without
the archive loses the design program; the archive without the bundle loses unpushed code.

## 5. Restore check (quarterly)

```bash
git clone ~/backups/jpa-metadata/repo-<date>.bundle /tmp/restore-test && rm -rf /tmp/restore-test
tar -tzf ~/backups/jpa-metadata/doc-<date>.tar.gz >/dev/null && echo "archive readable"
```

## 6. Retire this runbook when R-P0 closes

R-P0 has two halves: durability (this runbook) and the publicness decision (R-P0.1 —
whether any of `_doc/` may be published, given that it quotes production internals).
If R-P0.1 ever resolves toward publication, sanitized documents move to `docs/` and enter
git normally; the corpus never does (see `_doc/adr/adr-corpus-restore-vs-reattest.md`).
Until then, steps 3–4 are the only thing standing between the program and a disk failure.
