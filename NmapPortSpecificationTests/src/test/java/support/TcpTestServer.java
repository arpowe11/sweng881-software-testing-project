package support;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Small localhost-only TCP fixture used by the graph-based tests.
 *
 * It can act as:
 *  - a plain TCP listener for OPEN-state testing, or
 *  - a small HTTP server for Nmap service/version detection.
 */
public final class TcpTestServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final byte[] response;
    private final boolean waitForRequest;
    private final Thread acceptThread;

    private volatile boolean running = true;

    private TcpTestServer(String responseText, boolean waitForRequest)
            throws IOException {

        InetAddress loopback = InetAddress.getByName("127.0.0.1");

        this.serverSocket =
                new ServerSocket(0, 20, loopback);

        this.serverSocket.setSoTimeout(250);

        this.response = responseText == null
                ? null
                : responseText.getBytes(StandardCharsets.US_ASCII);

        this.waitForRequest = waitForRequest;

        this.acceptThread =
                new Thread(this::acceptLoop, "nmap-test-tcp-server");

        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    /**
     * Plain listening TCP socket for GT-TC1.
     */
    public static TcpTestServer listening() throws IOException {
        return new TcpTestServer(null, false);
    }

    /**
     * Small HTTP fixture for GT-TC6.
     *
     * The Server header intentionally resembles Python's SimpleHTTPServer
     * because Nmap has a known service fingerprint for it.
     */
    public static TcpTestServer httpServer() throws IOException {

        String response =
                "HTTP/1.0 200 OK\r\n" +
                        "Server: SimpleHTTP/0.6 Python/3.13.14\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" +
                        "<html><body>SWENG 881 Nmap test</body></html>\r\n";

        return new TcpTestServer(response, true);
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public String getHost() {
        return serverSocket.getInetAddress().getHostAddress();
    }

    private void acceptLoop() {

        while (running) {

            try (Socket socket = serverSocket.accept()) {

                handleConnection(socket);

            } catch (SocketTimeoutException ignored) {

                // Periodically wake so close() can terminate the thread.

            } catch (IOException e) {

                /*
                 * accept() itself failed. That's fatal only if the server
                 * socket was actually closed (normal shutdown via close());
                 * otherwise keep looping rather than killing the fixture
                 * over a transient error.
                 */
                if (serverSocket.isClosed()) {
                    running = false;
                }
            }
        }
    }

    /**
     * Handles a single accepted connection. Bare probes (e.g. Nmap's plain
     * connect-scan open/close, with no data exchanged) can cause the read or
     * write below to throw a non-timeout IOException (connection reset, EOF,
     * broken pipe). Those are per-connection failures and must not escape to
     * acceptLoop(), or a single bad probe would kill the fixture for every
     * subsequent connection.
     */
    private void handleConnection(Socket socket) {

        if (response == null) {
            return;
        }

        try {
            /*
             * Give Nmap an opportunity to send its HTTP probe.
             * Some service-detection probes connect before
             * immediately sending data.
             */
            if (waitForRequest) {

                socket.setSoTimeout(500);

                try {
                    byte[] buffer = new byte[4096];
                    socket.getInputStream().read(buffer);
                } catch (IOException ignored) {
                    /*
                     * Nmap may perform a probe that sends no data, or close
                     * the connection before we finish reading. Still return
                     * the HTTP banner so that service detection can inspect it.
                     */
                }
            }

            socket.getOutputStream().write(response);
            socket.getOutputStream().flush();

        } catch (IOException ignored) {
            // Peer disconnected before/while we responded; nothing to do.
        }
    }

    @Override
    public void close() {

        running = false;

        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }

        try {
            acceptThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}