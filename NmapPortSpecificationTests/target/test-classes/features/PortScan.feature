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
