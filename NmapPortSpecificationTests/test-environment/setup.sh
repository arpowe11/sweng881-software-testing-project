#!/usr/bin/env bash
# Sets up the controlled firewall environment needed for GT-TC3 and GT-TC4
# (the two @requires_root graph-based test cases whose expected classification
# is FILTERED). Loads pf rules that either silently drop or ICMP-reject
# traffic to two specific local ports so Nmap has something real to observe.
#
# Run with: sudo ./setup.sh
# Undo with: sudo ./teardown.sh

set -euo pipefail
# -e: stop immediately on any failing command instead of continuing past it.
# -u: treat use of an unset variable as an error, not an empty string.
# -o pipefail: a pipeline (a | b) fails if *any* stage fails, not just the last one.
# Firewall setup is exactly the kind of script where "kept going after
# something went wrong" is worse than "stopped immediately."

if [ "$EUID" -ne 0 ]; then
    # pfctl requires root to load rules or change pf's enabled state.
    # Fail fast with a clear instruction instead of letting pfctl fail
    # further down with a less obvious permission error.
    echo "Run with sudo: sudo $0" >&2
    exit 1
fi

# Resolve paths relative to *this script's* location, not the caller's
# current directory, so `sudo test-environment/setup.sh` works the same
# whether it's run from the repo root, from inside test-environment/, or
# from anywhere else.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RULES_FILE="$SCRIPT_DIR/pf-rules.conf"
STATE_FILE="$SCRIPT_DIR/.pf-was-enabled"

# Record whether pf was already enabled *before* we touch anything. teardown.sh
# reads this back so it can restore the exact prior state (leave pf enabled if
# it already was, or turn it back off if it wasn't) instead of assuming
# "disable pf" is always the right thing to do on cleanup.
if pfctl -s info | grep -q "Status: Enabled"; then
    echo "true" > "$STATE_FILE"
else
    echo "false" > "$STATE_FILE"
fi

if [ "$(cat "$STATE_FILE")" = "false" ]; then
    # Only enable pf if it was off. If it was already on (e.g. macOS is using
    # it for the Application Firewall or Internet Sharing), leave it alone -
    # calling pfctl -e again is harmless but unnecessary.
    pfctl -e
fi

# Load the rules into a *named anchor* (nmap.testing) rather than replacing
# the system's main ruleset with `pfctl -f`. An anchor is an isolated,
# independently loadable/flushable rule set - this way we never touch
# whatever pf rules macOS or other tools already have in place, and
# teardown.sh can cleanly remove exactly what we added and nothing else.
pfctl -a nmap.testing -f "$RULES_FILE"

echo "Loaded rules into anchor nmap.testing:"
pfctl -a nmap.testing -s rules
