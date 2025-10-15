#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   export GH_TOKEN=ghp_xxx   # GitHub PAT with repo scope
#   ./scripts/gh-auth.sh

if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "GH_TOKEN is not set. Export a GitHub token with 'repo' scope." 1>&2
  exit 1
fi

echo "Authenticating GitHub CLI using GH_TOKEN..."
echo "$GH_TOKEN" | gh auth login --with-token
echo "GitHub CLI is authenticated."


