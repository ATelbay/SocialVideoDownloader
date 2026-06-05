#!/usr/bin/env bash
# ============================================================================
# agent-assisted-setup.sh — setup/repair checklist for the agent workflow.
# Platform: macOS / Linux / Git-Bash. Windows native → agent-assisted-setup.ps1.
#
# Idempotent. Nothing irreversible. Personal tokens are NEVER touched.
# Steps:
#   1. Check/install optional CLIs (gh, jq) when possible.
#   2. Link shared agent skills where needed.
#   3. Generate per-agent contract bridges and personal-rules stub.
#   4. Print manual next steps.
#
# Most modern agents read AGENTS.md + .agents/skills natively (codex, cursor,
# windsurf, gemini, opencode, pi, copilot, roo, warp). For native readers this
# script mostly creates the personal layer; Pi also gets a local personal-rules
# bridge. Per-agent bridges are generated only where needed:
#   - Claude → root CLAUDE.md (@import) + .claude/skills symlink
#   - Gemini → .gemini/settings.json (context.fileName)
#   - Pi     → .pi/APPEND_SYSTEM.md symlink to ai/personal/AGENTS.md
#   - Kiro   → .kiro/skills symlink
#   - Cline  → .cline/skills symlink
# A new/unknown agent can be wired with --skills-dir / --entrypoint.
# ============================================================================
set -euo pipefail

AGENT=""
DO_INSTALL=1
SKILLS_DIR=""
ENTRYPOINT=""
while [ "$#" -gt 0 ]; do
	case "$1" in
	--no-install) DO_INSTALL=0 ;;
	--agent=*) AGENT="${1#*=}" ;;
	--agent)
		AGENT="${2:-}"
		shift
		;;
	--skills-dir=*) SKILLS_DIR="${1#*=}" ;;
	--skills-dir)
		SKILLS_DIR="${2:-}"
		shift
		;;
	--entrypoint=*) ENTRYPOINT="${1#*=}" ;;
	--entrypoint)
		ENTRYPOINT="${2:-}"
		shift
		;;
	-h | --help)
		awk 'NR == 1 { next } /^#/ { sub(/^# ?/, ""); print; next } { exit }' "$0"
		exit 0
		;;
	-*)
		echo "Unknown flag: $1" >&2
		exit 2
		;;
	*) AGENT="$1" ;;
	esac
	shift || break
done

KNOWN_AGENTS="claude codex cursor windsurf gemini opencode kiro cline pi copilot roo warp"
is_known() { case " $KNOWN_AGENTS " in *" $1 "*) return 0 ;; *) return 1 ;; esac }

if [ -z "$AGENT" ]; then
	echo "Specify agent, e.g. claude | codex | cursor | gemini | opencode | kiro | cline | pi" >&2
	echo "  example: scripts/agent-assisted-setup.sh pi" >&2
	exit 2
fi
if ! is_known "$AGENT" && [ -z "$SKILLS_DIR" ] && [ -z "$ENTRYPOINT" ]; then
	echo "Unknown agent: $AGENT" >&2
	echo "  For a new agent use Tier-2 fallback:" >&2
	echo "  scripts/agent-assisted-setup.sh $AGENT --skills-dir .$AGENT/skills --entrypoint <ENTRYPOINT>" >&2
	exit 2
fi

ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || {
	echo "Not a git repository." >&2
	exit 1
}
cd "$ROOT"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
ok() { printf '  \033[32m✓\033[0m %s\n' "$*"; }
skip() { printf '  \033[90m•\033[0m %s\n' "$*"; }
warn() { printf '  \033[33m⚠\033[0m %s\n' "$*"; }
have() { command -v "$1" >/dev/null 2>&1; }

SHARED_SKILLS_ABS="$ROOT/.agents/skills"
NATIVE_SKILL_AGENTS="codex cursor windsurf gemini opencode pi copilot roo warp"

agent_skills_dir() {
	case "$1" in
	claude) echo ".claude/skills" ;;
	kiro) echo ".kiro/skills" ;;
	cline) echo ".cline/skills" ;;
	*) echo "" ;;
	esac
}
is_native_skills_agent() { case " $NATIVE_SKILL_AGENTS " in *" $1 "*) return 0 ;; *) return 1 ;; esac }

relative_path() {
	local from="$1" to="$2"
	if have python3; then
		python3 - "$from" "$to" <<'PY'
import os, sys
print(os.path.relpath(sys.argv[2], start=sys.argv[1]))
PY
		return
	fi
	local common="$from" up=""
	while [ "${to#"$common"/}" = "$to" ] && [ "$to" != "$common" ]; do
		common="$(dirname "$common")"
		up="../$up"
	done
	if [ "$to" = "$common" ]; then printf '%s\n' "${up%/}"; elif [ "$common" = "/" ]; then printf '%s\n' "${up}${to#/}"; else printf '%s\n' "${up}${to#"$common"/}"; fi
}

link_skills_dir() {
	local rel="$1" target="$ROOT/$rel" parent shared_rel
	if [ ! -d "$SHARED_SKILLS_ABS" ]; then
		warn "shared skills dir not found: $SHARED_SKILLS_ABS"
		return 1
	fi
	parent="$(dirname "$target")"
	mkdir -p "$parent"
	shared_rel="$(relative_path "$parent" "$SHARED_SKILLS_ABS")" || return 1
	if [ -L "$target" ]; then rm "$target"; elif [ -e "$target" ]; then
		warn "$rel exists and is not a symlink — leaving it untouched"
		return 1
	fi
	ln -s "$shared_rel" "$target"
	ok "skills linked: $rel -> $shared_rel"
}

setup_skills() {
	local rel=""
	if [ -n "$SKILLS_DIR" ]; then
		link_skills_dir "$SKILLS_DIR" || warn "could not link $SKILLS_DIR"
		warn "Tier-2: add '$SKILLS_DIR' to .gitignore if it is local-only"
		return
	fi
	rel="$(agent_skills_dir "$AGENT")"
	if [ -n "$rel" ]; then
		link_skills_dir "$rel" || warn "could not link $rel"
	elif is_native_skills_agent "$AGENT"; then
		ok "$AGENT reads .agents/skills natively — no symlink needed"
	else
		skip "unknown agent '$AGENT' for skills — pass --skills-dir"
	fi
}

bold "1/4 · Optional CLI tools (gh, jq)"
install_one() {
	local bin="$1" formula="$2" name="$3" url="$4"
	if have "$bin"; then
		ok "$name already installed"
		return
	fi
	if [ "$DO_INSTALL" -eq 0 ]; then
		warn "$name not installed (skipped by --no-install)"
		return
	fi
	if have brew; then
		echo "  installing $name…"
		brew install "$formula" >/dev/null 2>&1 || true
		have "$bin" && ok "$name installed" || warn "could not install $name — install manually: brew install $formula ($url)"
	else
		warn "$name not installed and brew unavailable — install manually: $url"
	fi
}
install_one gh gh "GitHub CLI" "https://cli.github.com/"
install_one jq jq "jq" "https://jqlang.github.io/jq/"

bold "2/4 · Shared skills for: $AGENT"
setup_skills

bold "3/4 · Personal rules and bridges"
if [ -e ai/personal/AGENTS.md ]; then
	skip "ai/personal/AGENTS.md already exists"
else
	mkdir -p ai/personal
	printf '# Local personal rules for this repository (gitignored).\n# Layered over root AGENTS.md; on conflict these rules win.\n# Add your rules below.\n' >ai/personal/AGENTS.md
	ok "ai/personal/AGENTS.md created"
fi

if [ "$AGENT" = "pi" ]; then
	pi_append=".pi/APPEND_SYSTEM.md"
	pi_personal_rel="../ai/personal/AGENTS.md"
	mkdir -p .pi
	if [ -L "$pi_append" ]; then
		rm "$pi_append"
		ln -s "$pi_personal_rel" "$pi_append" && ok "$pi_append -> $pi_personal_rel"
	elif [ -e "$pi_append" ]; then
		warn "$pi_append already exists and is not a symlink — leaving it untouched"
	else
		ln -s "$pi_personal_rel" "$pi_append" && ok "$pi_append -> $pi_personal_rel"
	fi
fi

if [ "$AGENT" = "claude" ]; then
	if [ -f CLAUDE.md ] && grep -q '^@AGENTS\.md$' CLAUDE.md && grep -q '^@ai/personal/AGENTS\.md$' CLAUDE.md; then
		skip "CLAUDE.md already configured"
	else
		printf '@AGENTS.md\n@ai/personal/AGENTS.md\n' >CLAUDE.md
		ok "CLAUDE.md configured"
	fi
fi

if [ "$AGENT" = "gemini" ]; then
	cfg=".gemini/settings.json"
	if [ -f "$cfg" ]; then
		grep -q 'AGENTS.md' "$cfg" && skip ".gemini/settings.json already references AGENTS.md" || warn "add AGENTS.md to .gemini/settings.json context.fileName"
	else
		mkdir -p .gemini
		printf '%s\n' '{' '  "context": {' '    "fileName": ["AGENTS.md", "GEMINI.md"]' '  }' '}' >"$cfg"
		ok ".gemini/settings.json configured"
	fi
fi

if [ -n "$ENTRYPOINT" ]; then
	if [ -L "$ENTRYPOINT" ] || [ -e "$ENTRYPOINT" ]; then
		skip "$ENTRYPOINT already exists — leaving it untouched"
	else
		dir="$(dirname "$ENTRYPOINT")"
		mkdir -p "$dir"
		rel="AGENTS.md"
		if [ "$dir" != "." ]; then
			depth=$(printf '%s' "$dir" | awk -F/ '{print NF}')
			rel=""
			i=0
			while [ "$i" -lt "$depth" ]; do
				rel="../$rel"
				i=$((i + 1))
			done
			rel="${rel}AGENTS.md"
		fi
		ln -s "$rel" "$ENTRYPOINT" && ok "$ENTRYPOINT -> $rel"
		warn "Tier-2: add '$ENTRYPOINT' to .gitignore if it is local-only"
	fi
fi

bold "4/4 · Manual next steps"
echo "  • If using GitHub, run: gh auth status  (or gh auth login)"
echo "  • Verify branch/status: git branch --show-current && git status -sb"
echo "  • For task context, use /session-start when requirements are unclear"
