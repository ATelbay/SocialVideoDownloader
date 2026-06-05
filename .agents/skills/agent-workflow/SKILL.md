---
name: agent-workflow
description: "Tool-neutral workflow for SocialVideoDownloader agents: root AGENTS.md policy, shared skills layout, compatibility symlinks, local personal rules, setup scripts, and source-of-truth guidance. Use when maintaining agent docs/scripts/skills or coordinating multi-agent work."
user-invocable: true
---

# Agent Workflow

Root `AGENTS.md` is intentionally short: it contains only always-on invariants. Procedures, examples, and long checklists live in `.agents/skills/<name>/SKILL.md`.

## Source of truth

- `AGENTS.md` — compact project constitution.
- `.agents/skills/` — shared tool-neutral skills. Prefer updating a skill over growing `AGENTS.md`.
- Per-agent bridges (generated/local, gitignored where possible): root `CLAUDE.md`, `.pi/APPEND_SYSTEM.md`, `.gemini/settings.json`, `.claude/skills`, `.kiro/skills`, `.cline/skills`.
- `ai/personal/AGENTS.md` — per-developer local overrides, gitignored.
- Existing Spec Kit prompts under `.claude/commands/` and `.codex/prompts/` remain separate from this agent workflow.

## Skills setup

Most agents (codex, cursor, windsurf, gemini, opencode, pi, copilot, roo, warp) discover `.agents/skills` directly and need no symlink. Agents that scan their own directory use a compatibility symlink/junction:

| Agent | Skills path |
|---|---|
| Claude Code | `.claude/skills` |
| Kiro | `.kiro/skills` |
| Cline | `.cline/skills` |

Run the idempotent repair script when wiring is missing:

```bash
./scripts/agent-assisted-setup.sh <agent>
# for a custom agent:
./scripts/agent-assisted-setup.sh <agent> --skills-dir <agent-skills-dir> --entrypoint <entrypoint>
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\agent-assisted-setup.ps1 <agent>
```

Generated symlinks/junctions and local personal files are gitignored.

## Session workflow

- For a new task, create todos and wait for explicit `start` / `старт` unless the user's phrasing clearly asks to execute immediately.
- For complex/unclear tasks, run `/session-start` and present a short Task Brief.
- Use natural language for subagents/teammates; keep ownership separated by module (`core/*`, `shared/*`, `feature/*`, `server/*`, `iosApp/*`).

## Maintenance rule

When a rule becomes long, conditional, example-heavy, or task-specific, move it into a skill and leave only a one-line pointer in `AGENTS.md`.
