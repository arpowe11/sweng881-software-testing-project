package stepdefinitions;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import support.NmapOutputParser;
import support.NmapPortResult;
import support.TcpTestServer;
import system.NmapScanner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PortStateSteps {

    private NmapScanner scanner = new NmapScanner();
    private TcpTestServer tcpServer;

    private String targetHost;
    private int selectedPort;
    private String selectedProtocol;

    // ---------------------------------------------------------------------
    // GT-TC1: OPEN
    // ---------------------------------------------------------------------

    @Given("a reachable target with a TCP service listening on the selected port")
    public void aTcpServiceIsListening() throws IOException {
        tcpServer = TcpTestServer.listening();
        targetHost = tcpServer.getHost();
        selectedPort = tcpServer.getPort();
        selectedProtocol = "tcp";
    }

    // ---------------------------------------------------------------------
    // GT-TC2: CLOSED
    // ---------------------------------------------------------------------

    @Given("a reachable target with no TCP service listening on the selected port")
    public void noTcpServiceIsListening() throws IOException {
        targetHost = "127.0.0.1";
        selectedProtocol = "tcp";

        // Ask the OS for an unused local port and immediately release it.
        // That gives the test a port which should reject a TCP connection.
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            selectedPort = socket.getLocalPort();
        }
    }

    // ---------------------------------------------------------------------
    // GT-TC3: FILTERED through a silent timeout path
    // ---------------------------------------------------------------------

    @Given("a reachable target with a TCP port silently dropped by a firewall")
    public void tcpPortIsSilentlyDropped() {
        targetHost = controlledHost();
        selectedPort = controlledPort("nmap.gt.tc3.port", 50003);
        selectedProtocol = "tcp";
    }

    // ---------------------------------------------------------------------
    // GT-TC4: FILTERED through an ICMP unreachable/reject path
    // ---------------------------------------------------------------------

    @Given("a reachable target with a TCP port rejected with an ICMP unreachable response")
    public void tcpPortIsRejectedWithIcmp() {
        targetHost = controlledHost();
        selectedPort = controlledPort("nmap.gt.tc4.port", 50004);
        selectedProtocol = "tcp";
    }

    // ---------------------------------------------------------------------
    // GT-TC5: OPEN|FILTERED UDP ambiguity
    // ---------------------------------------------------------------------

    @Given("a reachable target with a UDP port that produces no response")
    public void udpPortProducesNoResponse() {
        targetHost = controlledHost();
        selectedPort = controlledPort("nmap.gt.tc5.port", 50005);
        selectedProtocol = "udp";
    }

    // ---------------------------------------------------------------------
    // GT-TC6: OPEN plus service/version detection
    // ---------------------------------------------------------------------

    @Given("a reachable target with an identifiable TCP service listening on the selected port")
    public void identifiableTcpServiceIsListening() throws IOException {

        tcpServer = TcpTestServer.httpServer();

        targetHost = tcpServer.getHost();
        selectedPort = tcpServer.getPort();
        selectedProtocol = "tcp";
    }

    // ---------------------------------------------------------------------
    // Scan actions
    // ---------------------------------------------------------------------

    @When("the selected port is scanned using a TCP connect scan")
    public void scanUsingTcpConnect() {
        scanner.scan("-sT -Pn -p " + selectedPort, targetHost);
    }

    @When("the selected port is scanned using a TCP SYN scan")
    public void scanUsingTcpSyn() {
        scanner.scan("-sS -Pn -p " + selectedPort, targetHost);
    }

    @When("the selected port is scanned using a UDP scan")
    public void scanUsingUdp() {
        scanner.scan("-sU -Pn -p " + selectedPort, targetHost);
    }

    @When("the selected port is scanned using TCP connect and version detection")
    public void scanUsingTcpVersionDetection() {
        scanner.scan(
                "-sT -sV --host-timeout 30s -Pn -p " + selectedPort,
                targetHost
        );
    }

    // ---------------------------------------------------------------------
    // Assertions
    // ---------------------------------------------------------------------

    @Then("Nmap shall report the selected TCP port as {string}")
    public void nmapReportsTcpState(String expectedState) {
        assertReportedState("tcp", expectedState);
    }

    @Then("Nmap shall report the selected UDP port as {string}")
    public void nmapReportsUdpState(String expectedState) {
        assertReportedState("udp", expectedState);
    }

    @Then("Nmap shall report service and version information for the selected TCP port")
    public void nmapReportsServiceAndVersion() {
        NmapPortResult result = resultFor("tcp");

        assertNotNull(result.service(), "Expected a SERVICE value in Nmap's port row");
        assertFalse(result.service().isBlank(), "Expected a non-blank SERVICE value");
        assertFalse(result.service().equalsIgnoreCase("unknown"),
                "Expected Nmap to identify the service rather than report 'unknown'");

        assertNotNull(result.version(),
                "Expected VERSION information from the -sV scan. Nmap output:\n"
                        + scanner.getOutput());
        assertFalse(result.version().isBlank(),
                "Expected non-blank VERSION information from the -sV scan");
    }

    private void assertReportedState(String protocol, String expectedState) {
        assertEquals(selectedProtocol, protocol,
                "Scenario protocol and assertion protocol do not match");

        NmapPortResult result = resultFor(protocol);
        assertEquals(expectedState.toLowerCase(), result.state().toLowerCase(),
                "Unexpected Nmap state for " + selectedPort + "/" + protocol
                        + ". Full Nmap output:\n" + scanner.getOutput());
    }

    private NmapPortResult resultFor(String protocol) {
        assertEquals(0, scanner.getExitCode(),
                "Expected Nmap to complete successfully. Output:\n" + scanner.getOutput());

        return NmapOutputParser.findPort(scanner.getOutput(), selectedPort, protocol)
                .orElseThrow(() -> new AssertionError(
                        "Nmap did not report a port-table row for "
                                + selectedPort + "/" + protocol
                                + ". Full Nmap output:\n" + scanner.getOutput()));
    }

    private static String controlledHost() {
        return System.getProperty("nmap.test.host", "127.0.0.1");
    }

    private static int controlledPort(String propertyName, int defaultPort) {
        String configured = System.getProperty(propertyName);
        if (configured == null || configured.isBlank()) {
            return defaultPort;
        }

        int port;
        try {
            port = Integer.parseInt(configured);
        } catch (NumberFormatException e) {
            throw new AssertionError(
                    "System property " + propertyName + " must be a TCP/UDP port number", e);
        }

        Assertions.assertTrue(port >= 1 && port <= 65535,
                "System property " + propertyName + " must be between 1 and 65535");
        return port;
    }

    @After
    public void cleanup() {

        if (tcpServer != null) {
            tcpServer.close();
            tcpServer = null;
        }
    }
}
