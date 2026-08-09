Feature: Nmap Port Specification Scanning

  Scenario: Scanning a single valid port
    Given a target host is reachable
    When I run Nmap with "-p 80"
    Then Nmap scans only port 80 and reports its state

  Scenario: Scanning a valid port range
    Given a target host is reachable
    When I run Nmap with "-p 1-1024"
    Then Nmap scans every port from 1 through 1024 and reports each port's state

  Scenario: Scanning a comma-separated list of ports
    Given a target host is reachable
    When I run Nmap with "-p 22,80,443"
    Then Nmap scans exactly those three ports and no others

  Scenario: Rejecting an out-of-range port
    Given a target host is reachable
    When I run Nmap with "-p 70000"
    Then Nmap rejects the command with a port-range error and does not attempt a scan

  Scenario: Scanning the full port-range wildcard
    Given a target host is reachable
    When I run Nmap with "-p-"
    Then Nmap completes a full-range scan without error

  @requires_root
  Scenario: Scanning a protocol-qualified mixed group
    Given a target host is reachable
    When I run Nmap with "-sU -sS -p U:53,T:80"
    Then Nmap scans UDP port 53 and TCP port 80 only

  Scenario: Rejecting malformed port syntax
    Given a target host is reachable
    When I run Nmap with "-p 22,,80"
    Then Nmap rejects the command with a syntax error and does not attempt a scan
