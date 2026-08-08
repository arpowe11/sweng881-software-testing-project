package support;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses rows from Nmap's normal human-readable port table, for example:
 *
 * 8080/tcp open     http    SimpleHTTPServer 0.6
 * 5000/udp open|filtered unknown
 *
 * Parsing the port row is stronger than checking whether the complete Nmap
 * output merely contains a word such as "open" or "filtered".
 */
public final class NmapOutputParser {

    private static final Pattern PORT_ROW = Pattern.compile(
            "(?m)^\\s*(\\d+)/(tcp|udp)\\s+(\\S+)\\s+(\\S+)(?:\\s+(.*\\S))?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private NmapOutputParser() {
    }

    public static Optional<NmapPortResult> findPort(
            String output,
            int expectedPort,
            String expectedProtocol) {

        if (output == null) {
            return Optional.empty();
        }

        Matcher matcher = PORT_ROW.matcher(output);
        while (matcher.find()) {
            int port = Integer.parseInt(matcher.group(1));
            String protocol = matcher.group(2);

            if (port == expectedPort
                    && protocol.equalsIgnoreCase(expectedProtocol)) {

                String version = matcher.group(5);
                if (version != null) {
                    version = version.trim();
                }

                return Optional.of(new NmapPortResult(
                        port,
                        protocol.toLowerCase(),
                        matcher.group(3).toLowerCase(),
                        matcher.group(4),
                        version));
            }
        }

        return Optional.empty();
    }
}
