package support;

/**
 * One row from Nmap's PORT / STATE / SERVICE table.
 */
public record NmapPortResult(
        int port,
        String protocol,
        String state,
        String service,
        String version) {
}
