@graph_testing @port_state_classification
Feature: Nmap port-state classification
  As a security tester
  I want Nmap to correctly classify a selected port
  So that I can identify the accessibility and availability of network services

  @GT_TC1 @tcp @open
  Scenario: GT-TC1 - Classify a listening TCP port as OPEN
    Given a reachable target with a TCP service listening on the selected port
    When the selected port is scanned using a TCP connect scan
    Then Nmap shall report the selected TCP port as "open"

  @GT_TC2 @tcp @closed
  Scenario: GT-TC2 - Classify a TCP port with no listening service as CLOSED
    Given a reachable target with no TCP service listening on the selected port
    When the selected port is scanned using a TCP connect scan
    Then Nmap shall report the selected TCP port as "closed"

  @GT_TC3 @tcp @filtered @requires_root @controlled_environment
  Scenario: GT-TC3 - Classify a silently dropped TCP port as FILTERED
    Given a reachable target with a TCP port silently dropped by a firewall
    When the selected port is scanned using a TCP SYN scan
    Then Nmap shall report the selected TCP port as "filtered"

  @GT_TC4 @tcp @filtered @requires_root @controlled_environment
  Scenario: GT-TC4 - Classify an ICMP-rejected TCP port as FILTERED
    Given a reachable target with a TCP port rejected with an ICMP unreachable response
    When the selected port is scanned using a TCP SYN scan
    Then Nmap shall report the selected TCP port as "filtered"

  @GT_TC5 @udp @open_filtered @requires_root @controlled_environment
  Scenario: GT-TC5 - Classify a silent UDP port as OPEN|FILTERED
    Given a reachable target with a UDP port that produces no response
    When the selected port is scanned using a UDP scan
    Then Nmap shall report the selected UDP port as "open|filtered"

  @GT_TC6 @tcp @service_detection
  Scenario: GT-TC6 - Identify service and version information on an OPEN TCP port
    Given a reachable target with an identifiable TCP service listening on the selected port
    When the selected port is scanned using TCP connect and version detection
    Then Nmap shall report the selected TCP port as "open"
    And Nmap shall report service and version information for the selected TCP port
