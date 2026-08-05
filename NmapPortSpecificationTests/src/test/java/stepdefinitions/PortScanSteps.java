package stepdefinitions;

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.en.*;
import system.NmapScanner;

public class PortScanSteps {

    private NmapScanner scanner;
    private static final String HOST = "localhost";

    @Given("a target host is reachable")
    public void a_target_host_is_reachable() {
        scanner = new NmapScanner();
    }

    @When("I run Nmap with {string}")
    public void i_run_nmap_with(String portArgs) {
        scanner.scan(portArgs, HOST);
    }

    @Then("Nmap scans only port 80 and reports its state")
    public void nmap_scans_only_port_80() {
        assertEquals(0, scanner.getExitCode(), "Expected a successful scan");
        assertTrue(scanner.getOutput().contains("80/tcp"),
                "Expected output to report a state for port 80");
    }

    @Then("Nmap scans every port from 1 through 1024 and reports each port's state")
    public void nmap_scans_range_1_to_1024() {
        assertEquals(0, scanner.getExitCode(), "Expected a successful scan");
        String out = scanner.getOutput();
        assertTrue(out.contains("PORT") && out.contains("STATE"),
                "Expected a port/state report table");
        assertTrue(out.contains("Nmap done"),
                "Expected Nmap to report completion");
    }

    @Then("Nmap scans exactly those three ports and no others")
    public void nmap_scans_exactly_three_ports() {
        assertEquals(0, scanner.getExitCode(), "Expected a successful scan");
        String out = scanner.getOutput();
        assertTrue(out.contains("22/tcp"), "Expected port 22 in output");
        assertTrue(out.contains("80/tcp"), "Expected port 80 in output");
        assertTrue(out.contains("443/tcp"), "Expected port 443 in output");
    }

    @Then("Nmap rejects the command with a port-range error and does not attempt a scan")
    public void nmap_rejects_out_of_range_port() {
        assertTrue(scanner.wasRejected(), "Expected a non-zero exit code for an invalid port");
        assertTrue(scanner.getOutput().toLowerCase().contains("between 0 and 65535"),
                "Expected a port-range error message");
    }
}
