---
name: merge-branch
description: "Safe workflow for merging a branch or pull request in SocialVideoDownloader via the gh CLI: branch naming, identifying the PR, waiting on CI checks, choosing squash, and the rule to NEVER bypass policy with --admin without explicit confirmation. Use when: asked to merge a PR or branch, completing a feature, finishing a review, resolving a stuck merge, or deciding between squash/merge/rebase."
user-invocable: true
---

# Merge a Branch / Pull Request

The default target is `main`. Always merge through a PR with the `gh` CLI;
**never push directly to `main`** (see `.claude/CLAUDE.md` git workflow).

## Branch naming

- Use `feature/{name}`, `fix/{name}`, `refactor/{name}`, `chore/{name}`, or `docs/{name}`.
- **Do not use the `codex/` prefix** — it is reserved for branches actually created by Codex.
  When working as Claude (or any non-Codex agent), pick the type prefix that matches the change.
- If a branch landed on the wrong prefix, rename it on GitHub **before** opening the PR
  (`gh api -X POST repos/<owner>/<repo>/branches/<old>/rename -f new_name=<new>`).
  Renaming a branch with an **already-open** PR can close that PR — rename first, open the PR after,
  or recreate the PR against the renamed branch.

## Process

1. **Identify the PR.** `gh pr list` (or `gh pr view <n>`). If the user said "merge it"
   without a number and there's exactly one open PR for the current branch, that's the one —
   but confirm the number out loud before acting.

2. **Inspect merge state** before attempting anything:
   ```bash
   gh pr checks <n>
   gh pr view <n> --json mergeable,mergeStateStatus,reviewDecision,baseRefName
   ```
   - `mergeable: MERGEABLE` only means *no conflicts*.
   - `mergeStateStatus: CLEAN` means nothing blocks the merge; `BLOCKED` means a gate is unmet.
   - `main` currently has **no GitHub branch protection** (no required checks/reviews enforced
     server-side), so a clean PR can be merged once CI is green. Treat green CI as the bar anyway —
     do not merge over failing checks.

3. **Wait for required CI** instead of force-merging:
   ```bash
   gh pr checks <n> --watch --interval 20
   ```
   The CI workflow (`.github/workflows/ci.yml`) runs **android-compile**, **android-lint**,
   **android-ktlint**, **shared-test**, **ios-build**, gated by a **changes** path filter.
   Docs/scripts-only PRs legitimately show the build jobs as `skipping` with only `changes` green —
   that is expected, not a failure.

4. **Merge once green** — squash is the repo default (linear history, one commit per PR,
   matches the `feat:`/`fix:`/`chore:` conventional-commit style):
   ```bash
   gh pr merge <n> --squash --delete-branch
   ```
   Use `--merge` only if the user explicitly wants every commit preserved; `--rebase` rarely.

5. **After merge**, sync local `main` and drop the deleted branch:
   ```bash
   git checkout main && git pull
   ```

## Merge-state blockers — what each means

| Symptom | Cause | Right action |
|---|---|---|
| `gh pr checks` shows `pending` | CI still running | `--watch`, then merge |
| Some check `fail` | Real CI failure | Fix it — do **not** override |
| Only `changes` ran, rest `skipping` | Path filter skipped build jobs (docs-only) | Expected — merge once `changes` passes |
| `mergeStateStatus: BEHIND` | Branch out of date with base | `gh pr update-branch <n>`, re-wait on checks |
| `mergeable: CONFLICTING` | Merge conflicts | See **Conflicts** below — do not merge |

## CRITICAL: never bypass policy without explicit confirmation

`gh pr merge --admin` overrides any branch protection. Even though `main` is currently
unprotected, **do not** reach for `--admin`:

- A user saying *"merge it"* authorizes a **normal** merge — it is **not** consent to override
  policy or to merge over failing/pending checks.
- Only run `--admin` after the user **explicitly** says to override / bypass / force the merge.
- Never use `--admin` to paper over a *failing* check — fix the underlying problem.
- If protection is later added to `main` and a required review is the only blocker, **stop and ask**
  whether to wait for a reviewer or admin-override.

## Conflicts

If `mergeable: CONFLICTING`, don't merge. Surface the conflicting files, and either resolve
locally on the feature branch (`git merge main`, fix, push) or let the user decide. Never
resolve conflicts by blindly taking one side.

> iOS note: before pushing changes that touch shared/KMP code, run the iOS native link locally
> so CI's `ios-build` doesn't fail on a K/N error you could have caught first.
