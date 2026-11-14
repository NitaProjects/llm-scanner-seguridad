package com.llmscannerseguridad.adapters.out.http.target;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsServer;

// Clave/certificado efímeros creados al ejecutar tests, sin archivos ni material operativo.
final class ControlledHttpsServer implements AutoCloseable {
    final SSLContext trustedContext;
    final AtomicInteger received = new AtomicInteger();
    final AtomicReference<String> hostHeader = new AtomicReference<>();
    final AtomicReference<String> sniHostname = new AtomicReference<>();
    private final HttpsServer server;
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

    ControlledHttpsServer() throws Exception {
        this("wrong-host.fixture.invalid");
    }

    ControlledHttpsServer(String hostname) throws Exception {
        this(hostname, exchange -> {
            byte[] bytes = "{\"text\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        });
    }

    ControlledHttpsServer(String hostname, com.sun.net.httpserver.HttpHandler handler) throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        byte[] algorithm = HexFormat.of().parseHex("300d06092a864886f70d01010b0500");
        byte[] name = new X500Principal("CN=" + hostname).getEncoded();
        var format = DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);
        byte[] validity = sequence(tag(0x17, format.format(Instant.now().minusSeconds(60)).getBytes(StandardCharsets.US_ASCII)),
                tag(0x17, format.format(Instant.now().plusSeconds(3600)).getBytes(StandardCharsets.US_ASCII)));
        byte[] subjectAlternativeName = sequence(HexFormat.of().parseHex("0603551d11"),
                tag(0x04, sequence(tag(0x82, hostname.getBytes(StandardCharsets.US_ASCII)))));
        byte[] tbs = sequence(tag(0xa0, tag(0x02, new byte[] {2})), tag(0x02, new byte[] {1}),
                algorithm, name, validity, name, pair.getPublic().getEncoded(), tag(0xa3, sequence(subjectAlternativeName)));
        var signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(pair.getPrivate());
        signature.update(tbs);
        byte[] signed = signature.sign();
        byte[] bitString = new byte[signed.length + 1];
        System.arraycopy(signed, 0, bitString, 1, signed.length);
        var certificate = CertificateFactory.getInstance("X.509").generateCertificate(
                new ByteArrayInputStream(sequence(tbs, algorithm, tag(0x03, bitString))));
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        char[] syntheticPassword = "fixture-only".toCharArray();
        keyStore.setKeyEntry("fixture", pair.getPrivate(), syntheticPassword, new java.security.cert.Certificate[] {certificate});
        var keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(keyStore, syntheticPassword);
        java.util.Arrays.fill(syntheticPassword, '\0');
        var serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keys.getKeyManagers(), null, null);
        var trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("fixture", certificate);
        var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(trustStore);
        trustedContext = SSLContext.getInstance("TLS");
        trustedContext.init(null, trust.getTrustManagers(), null);
        server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 8);
        server.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            received.incrementAndGet();
            hostHeader.set(exchange.getRequestHeaders().getFirst("Host"));
            var session = (ExtendedSSLSession) ((HttpsExchange) exchange).getSSLSession();
            session.getRequestedServerNames().stream().filter(SNIHostName.class::isInstance)
                    .map(SNIHostName.class::cast).findFirst().ifPresent(nameValue -> sniHostname.set(nameValue.getAsciiName()));
            try (exchange) {
                handler.handle(exchange);
            }
        });
        server.start();
    }

    String origin() { return "https://127.0.0.1:" + server.getAddress().getPort(); }

    private static byte[] sequence(byte[]... parts) {
        var output = new ByteArrayOutputStream();
        for (byte[] part : parts) output.writeBytes(part);
        return tag(0x30, output.toByteArray());
    }

    private static byte[] tag(int tag, byte[] value) {
        var output = new ByteArrayOutputStream();
        output.write(tag);
        if (value.length < 128) {
            output.write(value.length);
        } else {
            int bytes = value.length > 65535 ? 3 : value.length > 255 ? 2 : 1;
            output.write(0x80 | bytes);
            for (int i = bytes - 1; i >= 0; i--) output.write((value.length >>> (i * 8)) & 255);
        }
        output.writeBytes(value);
        return output.toByteArray();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
