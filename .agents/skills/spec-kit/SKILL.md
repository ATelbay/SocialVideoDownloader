---
name: spec-kit
description: "Spec Kit workflow for SocialVideoDownloader: when to use specs, commands for specify/plan/tasks/implement, and how to keep AGENTS.md updated without overwriting manual agent workflow rules. Use for new feature planning."
user-invocable: true
---

# Spec Kit

Use Spec Kit for new features or large planned work. Bug fixes and small tweaks can be direct implementation.

## Key paths

- `.specify/memory/constitution.md` — high-level principles.
- `specs/{NNN-feature-name}/` — generated `spec.md`, `plan.md`, `tasks.md`.
- `.claude/commands/` and `.codex/prompts/` — slash/prompt command assets.

## Typical flow

```bash
.specify/scripts/bash/create-new-feature.sh --json --short-name "<short-name>" "<feature description>"
.specify/scripts/bash/setup-plan.sh --json
.specify/scripts/bash/update-agent-context.sh codex
```

Then fill/maintain `spec.md`, `plan.md`, and `tasks.md` according to the generated templates.

## Important

- If `update-agent-context.sh` rewrites `AGENTS.md`, preserve the compact agent workflow structure and move long generated details into skills when necessary.
- Do not invent requirements; ask during clarify/planning.
- Keep implementation traceable to tasks and acceptance criteria.
