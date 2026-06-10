---
name: merge-branch
description: "Safe workflow for merging a branch or pull request in SocialVideoDownloader via the gh CLI: branch naming, identifying the PR, waiting on CI checks, choosing squash, and the rule to NEVER bypass policy with --admin without explicit confirmation. Use when: asked to merge a PR or branch, completing a feature, finishing a review, resolving a stuck merge, or deciding between squash/merge/rebase."
---

# Merge a Branch / Pull Request (bridge)

This is a thin Claude Code bridge. The canonical, agent-agnostic skill lives at
`.agents/skills/merge-branch/SKILL.md` — read that file now and follow it exactly.

Key invariants (duplicated here only so they survive a failed read):

- Always merge through a PR with `gh`; never push directly to `main`.
- Wait for CI (`gh pr checks <n> --watch --interval 20`) — never merge over failing
  or pending checks.
- Squash is the repo default: `gh pr merge <n> --squash --delete-branch`.
- NEVER use `--admin` unless the user explicitly asks to override/bypass policy.
- After merging: `git checkout main && git pull`.

If this bridge and the canonical file disagree, the canonical file wins.
