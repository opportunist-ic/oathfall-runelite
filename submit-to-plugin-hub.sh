#!/usr/bin/env bash
#
# Oathfall — one-shot Plugin Hub submission.
#
#   1. creates a public GitHub repo for the plugin and pushes it
#   2. rewrites support= / README links to your real GitHub username first,
#      because the Plugin Hub pins an exact commit hash
#   3. forks runelite/plugin-hub, adds plugins/oathfall, opens the pull request
#
# Run `gh auth login` once before this. Safe to re-run: it skips work already done.
#
set -euo pipefail

GH="${GH_BIN:-/c/Users/Danie/AppData/Local/Temp/claude/gh/bin/gh.exe}"
REPO_NAME="oathfall-runelite"
PLUGIN_ID="oathfall"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

command -v "$GH" >/dev/null 2>&1 || [ -x "$GH" ] || { echo "gh not found at $GH — set GH_BIN"; exit 1; }

echo "==> checking authentication"
"$GH" auth status >/dev/null 2>&1 || { echo "Not logged in. Run:  $GH auth login"; exit 1; }

USER_LOGIN="$("$GH" api user --jq .login)"
echo "    authenticated as $USER_LOGIN"

cd "$HERE"

# ---------------------------------------------------------------- 1. fix URLs
REPO_URL="https://github.com/$USER_LOGIN/$REPO_NAME"
echo "==> pointing support links at $REPO_URL"

sed -i "s|^support=.*|support=$REPO_URL/issues|" runelite-plugin.properties
sed -i "s|https://github.com/<user>/oathfall-runelite.git|$REPO_URL.git|g" README.md
sed -i "s|https://github.com/khentimentu/oathfall-runelite|$REPO_URL|g" README.md

if ! git diff --quiet; then
	git add -A
	git -c commit.gpgsign=false commit -q -m "Point support links at $USER_LOGIN/$REPO_NAME"
	echo "    committed link fixes"
else
	echo "    already correct"
fi

# ---------------------------------------------------------------- 2. push repo
if "$GH" repo view "$USER_LOGIN/$REPO_NAME" >/dev/null 2>&1; then
	echo "==> repo already exists, pushing"
	git remote get-url origin >/dev/null 2>&1 || git remote add origin "$REPO_URL.git"
	git push -u origin master
else
	echo "==> creating public repo $USER_LOGIN/$REPO_NAME"
	"$GH" repo create "$REPO_NAME" \
		--public \
		--source=. \
		--remote=origin \
		--description "Ledger, binding watchdog and companion tracker for the Oathfall hardcore ironman covenant" \
		--push
fi

COMMIT="$(git rev-parse HEAD)"
echo "    HEAD = $COMMIT"

# ---------------------------------------------------------------- 3. verify build
echo "==> building once more before submitting"
./gradlew build --no-daemon --console=plain -q || { echo "BUILD FAILED — not submitting"; exit 1; }
echo "    build OK"

# ---------------------------------------------------------------- 4. plugin-hub
WORK="$(mktemp -d)"
echo "==> forking runelite/plugin-hub"
# --remote is rejected when a repository argument is given, and a failure here
# must not be swallowed as "already exists" — that hides real errors.
if "$GH" repo view "$USER_LOGIN/plugin-hub" >/dev/null 2>&1; then
	echo "    fork already exists"
else
	"$GH" repo fork runelite/plugin-hub --clone=false
	sleep 6
fi

echo "==> cloning your fork"
"$GH" repo clone "$USER_LOGIN/plugin-hub" "$WORK/plugin-hub" -- -q
cd "$WORK/plugin-hub"

# The clone has no identity; use the GitHub noreply address so a public commit
# never carries a personal email.
USER_ID="$("$GH" api user --jq .id)"
git config user.name "$USER_LOGIN"
git config user.email "${USER_ID}+${USER_LOGIN}@users.noreply.github.com"

git remote add upstream https://github.com/runelite/plugin-hub.git 2>/dev/null || true
git fetch -q upstream
git checkout -q -B "$PLUGIN_ID" upstream/master

printf 'repository=%s.git\ncommit=%s\n' "$REPO_URL" "$COMMIT" > "plugins/$PLUGIN_ID"
echo "--- plugins/$PLUGIN_ID ---"
cat "plugins/$PLUGIN_ID"

git add "plugins/$PLUGIN_ID"
git -c commit.gpgsign=false commit -q -m "Add $PLUGIN_ID"
git push -q -f -u origin "$PLUGIN_ID"

echo "==> opening the pull request"
"$GH" pr create \
	--repo runelite/plugin-hub \
	--base master \
	--head "$USER_LOGIN:$PLUGIN_ID" \
	--title "Add oathfall" \
	--body "$(cat <<'BODY'
Adds **Oathfall**, a tracker for a hardcore-ironman challenge mode.

The plugin keeps the run's ledger (Doom track, Grace, Scars, Kept Oaths), deals
the challenge's Vow table, watches the sworn restriction for observable breaks,
and advances tier gates from real stat levels.

**No gameplay automation.** Every check is passive — it reads game state and
`MenuOptionClicked` events the player has already generated. Nothing sends input
to the client.

**Networking:** the plugin can optionally serve a small companion tracker page
over HTTP. It is **off by default**, binds to `127.0.0.1` only, requires a token
minted fresh each session, and makes no outbound connections of any kind.

Builds clean against RuneLite 1.12.36 on JDK 11 with no deprecation warnings.
BODY
)"

echo
echo "==> done. Pull request opened against runelite/plugin-hub."
echo "    Watch CI on the PR; if it fails, fix, push to $REPO_NAME, then update"
echo "    commit= in plugins/$PLUGIN_ID and push the branch again."
