package support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class PythonHttpTestServer implements AutoCloseable {

    private final Process process;
    private final int port;

    private PythonHttpTestServer(Process process, int port) {
        this.process = process;
        this.port = port;
    }

    public static PythonHttpTestServer start() throws IOException {

        int port = findAvailablePort();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "python",
                "-m",
                "http.server",
                String.valueOf(port),
                "--bind",
                "127.0.0.1"
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        /*
         * Drain Python's output so its output buffer can never fill
         * and block the server process.
         */
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream(),
                                         StandardCharsets.UTF_8))) {

                while (reader.readLine() != null) {
                    // Output intentionally discarded.
                }

            } catch (IOException ignored) {
            }
        });

        outputThread.setDaemon(true);
        outputThread.start();

        waitUntilReady(process, port);

        return new PythonHttpTestServer(process, port);
    }

    private static int findAvailablePort() throws IOException {

        try (ServerSocket socket =
                     new ServerSocket(
                             0,
                             1,
                             InetAddress.getByName("127.0.0.1"))) {

            return socket.getLocalPort();
        }
    }

    private static void waitUntilReady(Process process, int port)
            throws IOException {

        long deadline = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < deadline) {

            if (!process.isAlive()) {
                throw new IOException(
                        "Python HTTP server exited before becoming ready."
                );
            }

            try (Socket ignored =
                         new Socket("127.0.0.1", port)) {

                return;

            } catch (IOException ignored) {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    throw new IOException(
                            "Interrupted while waiting for Python HTTP server.",
                            e
                    );
                }
            }
        }

        process.destroyForcibly();

        throw new IOException(
                "Python HTTP server did not become ready within 5 seconds."
        );
    }

    public String getHost() {
        return "127.0.0.1";
    }

    public int getPort() {
        return port;
    }

    @Override
    public void close() {

        if (process.isAlive()) {

            process.destroy();

            try {
                if (!process.waitFor(
                        2,
                        java.util.concurrent.TimeUnit.SECONDS)) {

                    process.destroyForcibly();
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}