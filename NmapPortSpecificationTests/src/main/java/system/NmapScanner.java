package system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around the real nmap command-line tool.
 * Runs "nmap <portFlag> <host>" and captures stdout/stderr and the exit code
 * so acceptance tests can assert on real Nmap behavior.
 */
public class NmapScanner {

    private String output;
    private int exitCode;

    public void scan(String portArgs, String host) {
        List<String> command = new ArrayList<>();
        command.add("nmap");
        // portArgs may be like "-p 80" or "-p 1-1024"; split on the first space
        for (String part : portArgs.trim().split("\\s+")) {
            command.add(part);
        }
        command.add(host);

        StringBuilder sb = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // merge stderr into stdout
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
            }
            exitCode = process.waitFor();
        } catch (Exception e) {
            sb.append("ERROR running nmap: ").append(e.getMessage());
            exitCode = -1;
        }
        this.output = sb.toString();
    }

    public String getOutput() {
        return output;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean wasRejected() {
        return exitCode != 0;
    }
}
