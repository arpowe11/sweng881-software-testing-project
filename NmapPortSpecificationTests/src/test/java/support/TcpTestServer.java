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
 * It can either act as a plain listener (enough for an Nmap -sT OPEN test)
 * or send a banner on every connection (used by the -sV test).
 */
public final class TcpTestServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final byte[] banner;
    private final Thread acceptThread;
    private volatile boolean running = true;

    private TcpTestServer(String bannerText) throws IOException {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        this.serverSocket = new ServerSocket(0, 20, loopback);
        this.serverSocket.setSoTimeout(250);
        this.banner = bannerText == null
                ? null
                : bannerText.getBytes(StandardCharsets.US_ASCII);

        this.acceptThread = new Thread(this::acceptLoop, "nmap-test-tcp-server");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    public static TcpTestServer listening() throws IOException {
        return new TcpTestServer(null);
    }

    public static TcpTestServer withBanner(String bannerText) throws IOException {
        return new TcpTestServer(bannerText);
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
                if (banner != null) {
                    socket.getOutputStream().write(banner);
                    socket.getOutputStream().flush();
                }
            } catch (SocketTimeoutException ignored) {
                // Periodically wake up so close() can stop the thread promptly.
            } catch (IOException e) {
                if (running) {
                    // The test assertion will show Nmap's observed result if the
                    // fixture unexpectedly becomes unavailable.
                    running = false;
                }
            }
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
