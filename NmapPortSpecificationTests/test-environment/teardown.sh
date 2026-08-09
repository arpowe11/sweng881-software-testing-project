#!/usr/bin/env bash
# Reverses setup.sh: removes the pf rules added for GT-TC3/GT-TC4 and puts
# pf back exactly how it was found (enabled or disabled) beforehand.
# Always run this after the root-scan test session is done - leaving a
# DROP/REJECT rule loaded would silently break unrelated traffic to those
# two ports on this machine.
#
# Run with: sudo ./teardown.sh

set -euo pipefail
# Same rationale as setup.sh: fail loudly and immediately rather than
# leaving pf in a half-cleaned-up state.

if [ "$EUID" -ne 0 ]; then
    # pfctl requires root for both flushing the anchor and toggling pf's
    # enabled state.
    echo "Run with sudo: sudo $0" >&2
    exit 1
fi

# Same path-resolution reasoning as setup.sh - resolve relative to the
# script's own location, not the caller's cwd, and read back the state file
# setup.sh wrote so we know what "restored" actually means.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STATE_FILE="$SCRIPT_DIR/.pf-was-enabled"

# Remove every rule we loaded into the nmap.testing anchor. `-F all` on an
# anchor only clears that anchor's rules - it does not touch the system's
# main ruleset or any other anchor, so this can't accidentally wipe out
# pf rules some other tool depends on.
pfctl -a nmap.testing -F all
echo "Flushed anchor nmap.testing"

if [ -f "$STATE_FILE" ]; then
    if [ "$(cat "$STATE_FILE")" = "false" ]; then
        # pf was OFF before setup.sh ran, so setup.sh is the one that turned
        # it on. Turn it back off to leave the machine as we found it,
        # rather than leaving pf enabled as an unintended side effect.
        pfctl -d
        echo "pf was disabled before setup.sh ran - disabled it again"
    fi
    # If the state file said "true", pf was already enabled before setup.sh
    # ran (by macOS or something else) - leave it enabled, do nothing.

    rm -f "$STATE_FILE"
else
    # No state file means setup.sh was never run (or its output was already
    # cleaned up). We have no record of the prior state, so the safest thing
    # is to not guess - leave pf's enabled/disabled state exactly as we
    # found it now.
    echo "No state file found - leaving pf's enabled/disabled state as-is" >&2
fi
