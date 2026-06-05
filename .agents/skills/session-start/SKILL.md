---
name: session-start
description: "Optional task intake checklist: assemble a Task Brief from the user request, specs, branch, and similar code; ask focused questions when scope is unclear. Use for new/complex tasks, vague requirements, or 'where do we start'."
user-invocable: true
---

# Session Start — task intake

Use for complex or unclear work. Skip for tiny obvious edits.

## 1. Gather sources

1. User request — the primary source.
2. Existing specs under `specs/` and `.specify/` if this is a feature.
3. Branch name / recent commits for hints.
4. Similar implementation in `feature/*`, `core/*`, `shared/*`, `iosApp/`, or `server/`.
5. Current agent memory/todos if available.

## 2. Fill Task Brief

Keep each item short:

- **Modules:** where work likely happens.
- **Type:** feature / UI-VM / data / navigation / bugfix / refactor / tests / infra.
- **Done means:** observable acceptance criteria.
- **Reference:** similar existing code or spec.
- **Skills:** which `.agents/skills/*` to load.
- **Open questions:** unknowns or decisions.

## 3. Ask if needed

If acceptance criteria, platform scope, storage/security impact, or UI source is unclear, ask 2-3 targeted questions. Do not invent scope.

## 4. Confirm

Show the Task Brief. Start implementation only after user confirmation or explicit `start` / `старт` when the request was not already an execution command.
