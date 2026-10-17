#!/usr/bin/env bash
set -euo pipefail

# Locate repo root
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

MODE="${1:---check}"

if [[ "$MODE" != "--check" && "$MODE" != "--sync" ]]; then
    echo "Usage: $0 [--check | --sync]"
    echo "  --check   Validate documentation synchronization against current app version (default)"
    echo "  --sync    Automatically sync version matrices and references, then run validation"
    exit 1
fi

BUILD_GRADLE="app/build.gradle.kts"
if [[ ! -f "$BUILD_GRADLE" ]]; then
    echo "Error: $BUILD_GRADLE not found." >&2
    exit 1
fi

VERSION_NAME=$(grep -E 'versionName\s*=' "$BUILD_GRADLE" | head -n 1 | sed -E 's/.*"([^"]+)".*/\1/')
VERSION_CODE=$(grep -E 'versionCode\s*=' "$BUILD_GRADLE" | head -n 1 | sed -E 's/[^0-9]*([0-9]+).*/\1/')

if [[ -z "$VERSION_NAME" || -z "$VERSION_CODE" ]]; then
    echo "Error: Could not extract versionName or versionCode from $BUILD_GRADLE." >&2
    exit 1
fi

MAJOR=$(echo "$VERSION_NAME" | cut -d. -f1)
MINOR=$(echo "$VERSION_NAME" | cut -d. -f2)
PREV_MINOR=$((MINOR - 1))

ACTIVE_BRANCH="v${MAJOR}.${MINOR}.x"
PREV_BRANCH="v${MAJOR}.${PREV_MINOR}.x"
UNSUPPORTED_BOUND="< v${MAJOR}.${PREV_MINOR}.0"

if [[ "$MODE" == "--sync" ]]; then
    echo "=== Syncing Documentation for v${VERSION_NAME} (code ${VERSION_CODE}) ==="
    
    # 1. Update SECURITY.md supported version rows
    if [[ -f "SECURITY.md" ]]; then
        python3 -c "
import re

sec_file = 'SECURITY.md'
with open(sec_file, 'r', encoding='utf-8') as f:
    content = f.read()

pattern = r'(\|\s*Version\s*\|\s*Supported\s*\|\s*Notes\s*\|\n\|\s*:---\s*\|\s*:---:\s*\|\s*:---\s*\|\n)(?:\|[^\n]+\n){4}'
replacement = (
    r'\g<1>'
    f'| \`${ACTIVE_BRANCH}\` | :white_check_mark: | Current active release branch |\n'
    f'| \`${PREV_BRANCH}\` | :white_check_mark: | Previous release branch (security fixes) |\n'
    f'| \`main\` | :white_check_mark: | Development branch |\n'
    f'| \`${UNSUPPORTED_BOUND}\` | :x: | Unsupported; please update to the latest release |\n'
)

new_content = re.sub(pattern, replacement, content)
with open(sec_file, 'w', encoding='utf-8') as f:
    f.write(new_content)
"
        echo "  [Synced] SECURITY.md supported version table"
    fi

    # 2. Update .github/ISSUE_TEMPLATE/bug_report.md
    BUG_TEMPLATE=".github/ISSUE_TEMPLATE/bug_report.md"
    if [[ -f "$BUG_TEMPLATE" ]]; then
        python3 -c "
import re

template_file = '${BUG_TEMPLATE}'
with open(template_file, 'r', encoding='utf-8') as f:
    content = f.read()

new_content = re.sub(r'- Sotto Version:\s*\[e\.g\.\s*v[0-9.]+\]', f'- Sotto Version: [e.g. v${VERSION_NAME}]', content)
with open(template_file, 'w', encoding='utf-8') as f:
    f.write(new_content)
"
        echo "  [Synced] ${BUG_TEMPLATE} version example"
    fi

    echo "=== Sync Complete. Running Verification Checks... ==="
    echo ""
fi

# Validation Checks
FAILED=0

echo "=== Verifying Documentation Sync for v${VERSION_NAME} (code ${VERSION_CODE}) ==="

# Check 1: CHANGELOG.md section header
if grep -q -E "^## \[(v)?${VERSION_NAME}\]" CHANGELOG.md; then
    echo "  [PASS] 1. CHANGELOG.md contains release section header for v${VERSION_NAME}"
else
    echo "  [FAIL] 1. CHANGELOG.md missing release section header for v${VERSION_NAME} (expected '## [v${VERSION_NAME}]' or '## [${VERSION_NAME}]')" >&2
    FAILED=1
fi

# Check 2: SECURITY.md lists active version branch
if grep -q -E "\|[[:space:]]*\`${ACTIVE_BRANCH}\`[[:space:]]*\|[[:space:]]*:white_check_mark:[[:space:]]*\|[[:space:]]*Current active release branch" SECURITY.md; then
    echo "  [PASS] 2. SECURITY.md lists ${ACTIVE_BRANCH} as active release branch"
else
    echo "  [FAIL] 2. SECURITY.md does not list ${ACTIVE_BRANCH} as active release branch" >&2
    FAILED=1
fi

# Check 3: docs/PLAY_STORE_LISTING.md has What's New and Apa yang Baru
PLAY_STORE_DOC="docs/PLAY_STORE_LISTING.md"
if grep -q -E "What's New \(v?${VERSION_NAME}\)" "$PLAY_STORE_DOC" && \
   grep -q -E "Apa yang Baru \(v?${VERSION_NAME}\)" "$PLAY_STORE_DOC"; then
    echo "  [PASS] 3. ${PLAY_STORE_DOC} contains What's New and Apa yang Baru for v${VERSION_NAME}"
else
    echo "  [FAIL] 3. ${PLAY_STORE_DOC} missing What's New (v${VERSION_NAME}) or Apa yang Baru (v${VERSION_NAME})" >&2
    FAILED=1
fi

# Check 4: .github/ISSUE_TEMPLATE/bug_report.md contains v<versionName>
BUG_TEMPLATE=".github/ISSUE_TEMPLATE/bug_report.md"
if grep -q "v${VERSION_NAME}" "$BUG_TEMPLATE"; then
    echo "  [PASS] 4. ${BUG_TEMPLATE} references v${VERSION_NAME}"
else
    echo "  [FAIL] 4. ${BUG_TEMPLATE} missing reference to v${VERSION_NAME}" >&2
    FAILED=1
fi

# Check 5: Stale UI string check: No obsolete "Voice & Language Settings"
STALE_FOUND=0
for f in PRIVACY_POLICY.md docs/CAREGIVER_GUIDE.md docs/PLAY_STORE_LISTING.md; do
    if grep -i -q "Voice & Language Settings" "$f"; then
        echo "  [FAIL] 5. Stale phrase 'Voice & Language Settings' found in $f" >&2
        STALE_FOUND=1
    fi
done
if [[ $STALE_FOUND -eq 0 ]]; then
    echo "  [PASS] 5. No obsolete 'Voice & Language Settings' references found"
else
    FAILED=1
fi

# Check 6: README.md project structure check: No model/ directory or ui/theme/
if grep -E -q "(\bmodel/|ui/theme/)" README.md; then
    echo "  [FAIL] 6. README.md contains references to non-existent 'model/' or obsolete 'ui/theme/'" >&2
    FAILED=1
else
    echo "  [PASS] 6. README.md project structure does not reference 'model/' or 'ui/theme/'"
fi

if [[ $FAILED -ne 0 ]]; then
    echo ""
    echo "Documentation sync validation FAILED. Run './scripts/sync_docs.sh --sync' or address failures manually." >&2
    exit 1
fi

echo ""
echo "All documentation sync checks PASSED."
exit 0
