package com.llmscannerseguridad.adapters.out.http.target;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Solo se inicia al ejecutar explícitamente tests. Nunca escucha fuera de loopback.
public final class ControlledTargetServer implements AutoCloseable {
    public record Received(String method, String body, String authorization, String apiKey, String ordinary, long receivedNanos) {
        @Override public String toString() { return "Received[fixture sintética]"; }
    }
    public final CopyOnWriteArrayList<Received> received = new CopyOnWriteArrayList<>();
    private final HttpServer server;
    private final java.util.concurrent.ExecutorService executor = Executors.newFixedThreadPool(2);

    public ControlledTargetServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 8);
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    public String origin() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String body = new String(exchange.getRequestBody().readNBytes(65537), StandardCharsets.UTF_8);
            var headers = exchange.getRequestHeaders();
            received.add(new Received(exchange.getRequestMethod(), body, headers.getFirst("Authorization"),
                    headers.getFirst("X-API-Key"), headers.getFirst("X-Demo"), System.nanoTime()));
            String path = exchange.getRequestURI().getPath();
            int status = path.startsWith("/status/") ? Integer.parseInt(path.substring(8)) : 200;
            if (status >= 300 && status < 400) {
                exchange.getResponseHeaders().set("Location", origin() + "/second");
            }
            String text = "{\"text\":\"ok\"}";
            if (path.equals("/sequence") && received.size() == 1) {
                text = "{invalid";
            } else if (path.equals("/echo")) {
                String auth = headers.getFirst("Authorization");
                String key = headers.getFirst("X-API-Key");
                text = "{\"text\":\"" + (auth == null ? key : auth) + "\"}";
            } else if (path.equals("/invalid")) {
                text = "{\"text\":";
            } else if (path.equals("/missing")) {
                text = "{}";
            } else if (path.equals("/nontext")) {
                text = "{\"text\":42}";
            } else if (path.equals("/large")) {
                text = "{\"text\":\"" + "x".repeat(300000) + "\"}";
            } else if (path.equals("/dangerous")) {
                text = "{\"text\":\"<script>inert fixture</script>\"}";
            } else if (path.equals("/nul")) {
                text = "{\"text\":\"a\\u0000😀\"}";
            } else if (path.equals("/slow")) {
                try { Thread.sleep(1500); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            } else if (path.startsWith("/text/")) {
                text = "{\"text\":\"" + "x".repeat(Integer.parseInt(path.substring(6))) + "\"}";
            } else if (path.equals("/duplicate")) {
                text = "{\"text\":\"x\",\"text\":\"y\"}";
            } else if (path.equals("/deep")) {
                text = "[".repeat(65) + "0" + "]".repeat(65);
            } else if (path.equals("/compressed")) {
                exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            } else if (path.equals("/manyheaders")) {
                for (int i = 0; i < 40; i++) exchange.getResponseHeaders().set("X-Fixture-" + i, "x".repeat(1000));
            } else if (path.equals("/slowbody")) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, 0);
                byte[] slow = "{\"text\":\"slow\"}".getBytes(StandardCharsets.UTF_8);
                for (byte part : slow) {
                    exchange.getResponseBody().write(part);
                    exchange.getResponseBody().flush();
                    try { Thread.sleep(200); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                }
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", path.equals("/format") ? "text/plain" : "application/json");
            byte[] bytes = path.equals("/utf8") ? new byte[] {(byte) 0xc3, 0x28} : text.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, path.equals("/large") ? 0 : bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
