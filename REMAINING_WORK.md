# SWENG 881 – Nmap Testing Project: Remaining Work

**Team:** Alexander Powell, Jacob Hoffman
**Scope split:** Alexander → Input Domain Modeling (`-p` port specification); Jacob → Graph-Based Testing (TCP port-state classification, user story *"Identify an Open TCP Port"*).

This document tracks everything still left to do to move from the current progress report to the final submission. It is organized as an ordered phase list. Items marked **DONE** are already in the repo; everything else is outstanding.

---

## Current State (baseline)

Implemented as a Cucumber + JUnit 5 (BDD) project driving the real `nmap` CLI:

- `src/main/java/system/NmapScanner.java` — wrapper that runs `nmap` and captures stdout/stderr + exit code. **DONE**
- `src/test/resources/features/PortScan.feature` — 4 scenarios (IDM blocks 1, 2, 3, 6). **DONE**
- `src/test/java/stepdefinitions/PortScanSteps.java` — step defs for those 4. **DONE**
- `src/test/java/testrunner/RunCucumberTest.java` — JUnit Platform Suite runner. **DONE**

**Coverage today:** 4 of 7 IDM blocks. 0 of 6 graph-based (port-state) test cases.

---

## Phase 1 — Complete the Input Domain Modeling test set (Alexander)

Add the three missing IDM blocks so Each Choice Coverage is actually satisfied in code (currently only satisfied on paper).

- [ ] **IDM-TC4 — full wildcard** (`-p-`): scenario + step. Assert exit 0 and a completed scan report (`Nmap done`). Don't assert all 65535 lines are printed — nmap only lists open/closed-reported ports; assert on completion + no error instead.
- [ ] **IDM-TC5 — protocol-qualified group** (`-sU -sS -p U:53,T:80`): scenario + step. Requires root (raw sockets). Either run under `sudo`, or document that this case is verified manually and skip/tag it in CI.
- [ ] **IDM-TC7 — malformed syntax** (`-p 22-,-80`): scenario + step. Assert `wasRejected()` and a syntax/parse error message. Verify the actual error string nmap emits (see Phase 3 note).
- [ ] Verify the IDM-TC6 assertion string (`"between 0 and 65535"`) matches the installed nmap version's real error text; adjust if needed.
- [ ] Consider generalizing `NmapScanner.scan(...)` or adding an overload so scan-type flags (`-sS`, `-sU`, `-sT`, `-sV`) compose cleanly for TC5 and all graph tests. (It already splits on whitespace, so multi-flag strings work; just confirm and document.)

## Phase 2 — Implement the Graph-Based (port-state classification) test set (Jacob)

The user story *"Identify an Open TCP Port"* is **GT-TC1**. Build out all edge-coverage cases from the report.

- [ ] Add a `PortState.feature` (or extend the existing feature) with scenarios GT-TC1–GT-TC6.
- [ ] **GT-TC1 — OPEN** (`-sT -p <listening port>`): the user story. Assert state reported as `open`. *(no root needed — connect scan)*
- [ ] **GT-TC2 — CLOSED** (`-sT -p <no listener>`): assert `closed`. *(no root)*
- [ ] **GT-TC3 — FILTERED, timeout path** (`-sS -p <firewall-dropped port>`): assert `filtered`. *(root)*
- [ ] **GT-TC4 — FILTERED, ICMP path** (`-sS -p <firewall-rejected, ICMP unreachable>`): assert `filtered`. *(root)*
- [ ] **GT-TC5 — OPEN|FILTERED** (`-sU -p <UDP port, no reply>`): assert `open|filtered`; document this is intentional ambiguity, not a defect. *(root)*
- [ ] **GT-TC6 — OPEN + service/version** (`-sV -sT -p <listening port>`): assert `open` and a service/version column populated. *(no root)*
- [ ] Add step definitions that parse the reported state from nmap output (e.g. regex on the `PORT STATE SERVICE` table) rather than raw `contains`.

## Phase 3 — Build the controlled target environment

This is the anticipated main challenge from the report. Needed so every block/edge can actually be exercised.

- [ ] Stand up a target with a **known open TCP port** (e.g. start a local service / `nc -l`) for GT-TC1, GT-TC6, IDM-TC1.
- [ ] Ensure a **known closed port** (no listener) for GT-TC2.
- [ ] Configure a **firewall DROP rule** (silent drop → timeout) for GT-TC3. (macOS `pf` / Linux `iptables -j DROP`.)
- [ ] Configure a **firewall REJECT rule** returning ICMP unreachable for GT-TC4. (`iptables -j REJECT --reject-with icmp-port-unreachable`.)
- [ ] Provide a **UDP port with no listener and no drop rule** for GT-TC5.
- [ ] Document the exact setup commands so results are reproducible (put in a `test-environment/` README or script). Consider a Docker/Vagrant image to make it portable and grader-reproducible.
- [ ] Confirm `sudo`/root strategy for the raw-socket scans (`-sS`, `-sU`).

## Phase 4 — Live execution & record actual results

Both sections currently report *expected* outcomes only. Convert to observed.

- [ ] Run the full IDM suite live; record actual state per test case.
- [ ] Run the full graph suite live against the Phase 3 environment; record actual classification per test case.
- [ ] Capture raw `nmap` output for each case (attach to final report).
- [ ] Reconcile any mismatch between expected and observed as a **defect with reproduction steps**.
- [ ] Make the whole suite green: `mvn test` passes end-to-end (or documents which cases are manual/root-only).

## Phase 5 — Documentation & artifacts

- [ ] Produce the **graph diagram** (nodes N0–N9, edges as described) as an actual image and attach it (report currently says "will be attached").
- [ ] Write up the **defects/observations** section with the OPEN|FILTERED ambiguity note and any real defects found in Phase 4.
- [ ] **Professor requirement:** add links to **both** repositories in the final report:
  - [ ] GitHub repo of the project being evaluated → **Nmap**: https://github.com/nmap/nmap
  - [ ] GitHub repo containing **our tests** → push this project to a public repo and link it. *(Repo is currently local / not git-initialized — see Phase 6.)*
- [ ] Update the report tables so the "Expected Result" columns become "Expected / Actual" once Phase 4 is done.

## Phase 6 — Repo hygiene & final submission

- [ ] `git init` this project and push to GitHub (needed for the professor's repo-link requirement).
- [ ] Add a `.gitignore` (exclude `target/`, `.idea/`, `.DS_Store`).
- [ ] Add a top-level `README.md`: how to install nmap, how to run the suite (`mvn test`), root-scan caveats, and the target-environment setup.
- [ ] Final proofread of the report incorporating the professor's feedback (IDM praised; add repo links; tests now implemented).

---

## Quick status summary

| Area | Planned | Implemented | Remaining |
|------|---------|-------------|-----------|
| IDM test cases | 7 | 4 (TC1,2,3,6) | TC4, TC5, TC7 |
| Graph/port-state cases | 6 | 0 | GT-TC1–6 (incl. the OPEN user story) |
| Target environment | — | none | Phases 3 |
| Live execution results | — | none (expected only) | Phase 4 |
| Graph diagram | — | described only | Phase 5 |
| Repo links (prof. req.) | — | none | Phase 5/6 |
