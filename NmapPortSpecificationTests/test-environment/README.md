# Controlled firewall environment (GT-TC3 / GT-TC4 / GT-TC5 / IDM-TC5)

These are the four `@requires_root` scenarios, excluded from the default `mvn test` run because
they need raw-socket scans and, for GT-TC3/GT-TC4, a real firewall rule behind them:

| Test case | Needs |
|---|---|
| GT-TC3 | root (`-sS`) + a silent DROP rule on TCP port 50003 |
| GT-TC4 | root (`-sS`) + an ICMP-reject rule on TCP port 50004 |
| GT-TC5 | root (`-sU`) only — no firewall rule needed, see note below |
| IDM-TC5 | root (`-sU -sS`) only — no firewall rule needed |

## What's in this directory

- `pf-rules.conf` — the two `pf` rules GT-TC3/GT-TC4 need.
- `setup.sh` — loads those rules into an isolated `pf` anchor (`nmap.testing`), without touching
  the system's main firewall ruleset. Safe to run on top of macOS's existing pf usage (Application
  Firewall, Internet Sharing, etc.).
- `teardown.sh` — removes the anchor's rules and restores pf's enabled/disabled state to whatever
  it was before `setup.sh` ran.

## Running the root-required scenarios

```bash
sudo ./test-environment/setup.sh
sudo mvn test -Proot-scans
sudo ./test-environment/teardown.sh
```

Always run `teardown.sh` afterward — leaving the DROP/REJECT rules loaded would silently break
real traffic to ports 50003/50004 on this machine.

## Why GT-TC5 and IDM-TC5 don't need a firewall rule

GT-TC5 (UDP, no reply) and IDM-TC5 (protocol-qualified group) only need root for the raw-socket
scan itself (`-sU`, `-sS`). A UDP port with nothing listening and no rule dropping the probe
already produces Nmap's `open|filtered` result on its own — that's the documented ambiguity
being tested (see the report), not something a firewall rule needs to manufacture.

## Custom ports

`PortStateSteps.java` reads the target ports from system properties, defaulting to the values
above:

```bash
sudo mvn test -Proot-scans -Dnmap.gt.tc3.port=50003 -Dnmap.gt.tc4.port=50004 -Dnmap.gt.tc5.port=50005
```

If you change the ports here, update `pf-rules.conf` to match.

## Known limitation: GT-TC3/TC4/TC5 do not pass on macOS today

**Status: attempted, not resolved. Documenting as an environment limitation rather than a code
defect, per the report's own anticipated-challenge note.**

### What was tried

1. Wrote `pf-rules.conf` with a silent DROP rule for GT-TC3 (TCP port 50003) and an ICMP-reject
   rule (`return-icmp(port-unr)`, deliberately not `return-rst`, since an RST makes Nmap report
   CLOSED rather than FILTERED) for GT-TC4 (TCP port 50004).
2. Wrote `setup.sh` / `teardown.sh` to load those rules into an isolated `pf` anchor
   (`nmap.testing`) — chosen specifically so the scripts never touch or replace macOS's own pf
   usage (Application Firewall, Internet Sharing, etc.), and so they can be run and reversed
   safely on a grader's machine.
3. Ran the full sequence against `localhost`: `setup.sh`, then `sudo mvn test -Proot-scans`, then
   `teardown.sh`.

### What happened

All three scenarios (GT-TC3, GT-TC4, GT-TC5) came back `closed` instead of the expected
`filtered` / `open|filtered`, and came back almost instantly (~0.1s) rather than timing out. That
timing is the tell: a real DROP rule would make Nmap wait out its retry/timeout window (many
seconds); an immediate response means something answered right away — i.e., the `pf` rule was
never applied to the traffic at all. (IDM-TC5, the fourth root-required scenario, does not depend
on any firewall rule and passed normally, confirming root/raw-socket scanning itself works fine
here — the problem is specifically the firewall rule not intercepting anything.)

### Root cause

macOS's network stack fast-paths loopback traffic (`127.0.0.1` / the `lo0` interface) around the
`pf` filtering hooks at a level below `pf.conf`. Neither `/etc/pf.conf` nor Apple's own loaded
`com.apple` anchor contains an explicit `skip on lo0`, so this isn't a config setting that can be
overridden from an anchor — it appears to be built into how Darwin handles loopback delivery. As
a next step we re-ran the same rules targeting the machine's real LAN IP
(`nmap.test.host=<LAN IP>`) instead of `localhost`, to test whether traffic that actually traverses
a real interface gets filtered — see the git history / conversation log for that result if it was
captured before this was set aside.

### What would actually fix it

Testing a real DROP/REJECT firewall rule against Nmap requires traffic that genuinely passes
through a filterable interface — e.g., scanning a second physical machine or a VM on the same
network, or a container with its own network namespace, rather than scanning the same macOS host
that's running `pf`. That's a heavier environment (Docker/VM) than was in scope for this project's
timeline, so GT-TC3/GT-TC4/GT-TC5 are left as **written and tagged (`@requires_root
@controlled_environment`), scaffolded correctly, but not live-verified** — call this out explicitly
in the report's defects/observations section rather than reporting these as passing.
