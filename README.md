# SWENG 881 – Nmap Software Testing Project

Software testing final project for SWENG 881. This project applies two black-box
test design techniques — Input Domain Modeling and Graph-Based Testing — to
[Nmap](https://nmap.org), the network mapping and port-scanning tool.

The tests are BDD-style acceptance tests (Cucumber + JUnit 5) that drive the real
`nmap` CLI against local, disposable TCP/UDP fixtures and assert on its actual
output.

- **Input Domain Modeling** (`PortScan.feature`) — exercises Nmap's `-p` port
  specification syntax (single port, range, list, wildcard, malformed input, etc.).
- **Graph-Based Testing** (`PortState.feature`) — exercises the user story
  *"Identify an Open TCP Port"*, covering the port-state classification graph
  (open, closed, filtered, open|filtered) and service/version detection.

## Requirements

- **nmap** — install via `brew install nmap` (macOS), `apt install nmap` (Debian/Ubuntu),
  or from [nmap.org/download](https://nmap.org/download.html).
- **JDK 17+**
- **Maven 3.8+**

## Running the tests

From `NmapPortSpecificationTests/`:

```bash
mvn test
```

By default this runs 7 of the 13 scenarios and is green out of the box. Scenarios
tagged `@requires_root` (GT-TC3, GT-TC4, GT-TC5) are excluded because they perform
raw-socket scans (`-sS`, `-sU`) that require root privileges and a controlled
firewall environment (drop/reject rules) to exercise the FILTERED and
OPEN|FILTERED states. Without root, `nmap -sS`/`-sU` falls back to a scan type
that doesn't exercise those code paths, so these scenarios are skipped rather than
run in a way that would silently fail or give a false result.

To opt in and run the root-required scenarios once a controlled firewall
environment is set up:

```bash
sudo mvn test -Proot-scans
```

## Repositories

- Project under test: [nmap/nmap](https://github.com/nmap/nmap)
- This test project: https://github.com/arpowe11/sweng881-software-testing-project
