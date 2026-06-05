---
name: initial-project-setup
description: "One-time setup/doctor for local agent wiring: run scripts/agent-assisted-setup.{sh,ps1}, verify shared skills, bridges, personal rules, optional GitHub CLI, and branch state. Use on fresh clone/new machine or when agent skills/entrypoints are broken."
user-invocable: true
---

# Initial project setup — agent wiring doctor

Use on a fresh clone, a new machine, or when local agent wiring is broken. This is not a feature intake; for task context use `/session-start`.

## 1. Determine agent and platform

- Agent: current runtime if obvious (`pi`, `claude`, `codex`, `gemini`, `cursor`, `kiro`, `cline`, ...). If not obvious, ask one short question.
- macOS/Linux/Git Bash:

```bash
scripts/agent-assisted-setup.sh <agent>
```

- Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\agent-assisted-setup.ps1 <agent>
```

Add `--no-install` / `-NoInstall` if the user does not want optional CLI install attempts.

## 2. What the setup script does

1. Checks optional CLIs (`gh`, `jq`) and installs through the platform package manager only when possible and not disabled.
2. Creates/repairs skills symlinks for agents that need them.
3. Creates `ai/personal/AGENTS.md` if missing.
4. Creates local bridges where useful: root `CLAUDE.md`, `.pi/APPEND_SYSTEM.md`, `.gemini/settings.json`, or custom Tier-2 entrypoints.
5. Prints manual next steps.

## 3. Doctor-check after setup

Report each item with `✓` / `⚠` / `✗` and a fix:

1. **Skills visible.** Native agents read `.agents/skills`; Claude/Kiro/Cline use symlink/junction.
2. **Entrypoint/bridge present.** Claude root `CLAUDE.md` imports `AGENTS.md` and `ai/personal/AGENTS.md`; Pi `.pi/APPEND_SYSTEM.md` points to personal rules; Gemini settings reference `AGENTS.md`.
3. **Personal layer created.** `ai/personal/AGENTS.md` exists.
4. **Optional CLI available.** `gh --version` and `jq --version` if the workflow needs GitHub/API JSON.
5. **Branch state safe.** `git branch --show-current` and `git status -sb`; never push directly to `main`.

## 4. Handoff

Finish with a concise summary:

```text
Agent setup doctor:
✓ ...
⚠ ... — fix: ...
✗ ... — fix: ...
```

Then remind: source of truth is `AGENTS.md`; task-specific process starts with `/session-start` when needed.
