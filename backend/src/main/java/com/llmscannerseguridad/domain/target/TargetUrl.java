package com.llmscannerseguridad.domain.target;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

// Analiza únicamente texto y literales IP: nunca resuelve nombres ni abre conexiones.
public final class TargetUrl {
    private final String value;
    private final String host;
    private final String scheme;
    private final int port;
    private final byte[] address;

    public TargetUrl(String raw) {
        TargetRules.noNullCharacter(raw, "url");
        if (raw != null && TargetRules.controls(raw)) {
            throw TargetValidationException.field("url", "INVALID_CHARACTER");
        }
        if (raw == null || TargetRules.trim(raw).isEmpty()) {
            throw TargetValidationException.field("url", "REQUIRED");
        }
        value = TargetRules.trim(raw);
        TargetRules.length(value, 2048, "url");
        if (value.codePoints().anyMatch(cp -> TargetRules.whitespace(cp) || cp <= 31 || cp == 127)) {
            throw TargetValidationException.field("url", "INVALID_FORMAT");
        }
        try {
            URI uri = new URI(value);
            scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                    || uri.getPort() < -1 || uri.getPort() > 65535 || uri.getPort() == 0
                    || uri.getRawAuthority().endsWith(":")) {
                throw TargetValidationException.field("url", "INVALID_FORMAT");
            }
            String parsedHost = uri.getHost().toLowerCase(Locale.ROOT);
            if (parsedHost.startsWith("[")) {
                parsedHost = parsedHost.substring(1, parsedHost.length() - 1);
            }
            if (parsedHost.contains("%")) {
                throw TargetValidationException.field("url", "INVALID_FORMAT");
            }
            while (parsedHost.endsWith(".")) {
                parsedHost = parsedHost.substring(0, parsedHost.length() - 1);
            }
            host = parsedHost;
            port = uri.getPort() == -1 ? (scheme.equals("https") ? 443 : 80) : uri.getPort();
            address = parseAddress(host);
        } catch (URISyntaxException error) {
            throw TargetValidationException.field("url", "INVALID_FORMAT");
        }
    }

    public String value() {
        return value;
    }

    public boolean sameOrigin(TargetUrl other) {
        return scheme.equals(other.scheme) && port == other.port
                && (address != null && other.address != null
                    ? Arrays.equals(address, other.address) : host.equals(other.host));
    }

    public String origin() {
        String normalized = address == null ? host : java.util.HexFormat.of().formatHex(address);
        return scheme + "://" + normalized + ":" + port;
    }

    public boolean metadataAddress() {
        if (address == null) {
            return host.equals("metadata.google.internal");
        }
        // Link-local, incluidas direcciones de metadatos: nunca se exceptúan.
        return address.length == 4 ? (unsigned(0) == 169 && unsigned(1) == 254)
                || Arrays.equals(address, new byte[] {100, 100, 100, (byte) 200})
                : (unsigned(0) == 254 && (unsigned(1) & 192) == 128)
                || Arrays.equals(address, parseAddress("fd00:ec2::254"));
    }

    public boolean restricted() {
        if (host.equals("localhost") || host.endsWith(".localhost")) {
            return true;
        }
        if (address == null) {
            return false;
        }
        if (address.length == 4) {
            return unsigned(0) == 0 || unsigned(0) == 10 || unsigned(0) == 127
                    || (unsigned(0) == 172 && unsigned(1) >= 16 && unsigned(1) <= 31)
                    || (unsigned(0) == 192 && unsigned(1) == 168) || metadataAddress();
        }
        boolean zeroPrefix = true;
        for (int i = 0; i < 15; i++) {
            zeroPrefix &= address[i] == 0;
        }
        return (zeroPrefix && unsigned(15) <= 1) || (unsigned(0) & 254) == 252 || metadataAddress();
    }

    private int unsigned(int index) {
        return Byte.toUnsignedInt(address[index]);
    }

    private static byte[] ipv4(String value) {
        if (!value.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
            return null;
        }
        var parts = value.split("\\.");
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            if (parts[i].length() > 3 || Integer.parseInt(parts[i]) > 255) {
                throw TargetValidationException.field("url", "INVALID_FORMAT");
            }
            result[i] = (byte) Integer.parseInt(parts[i]);
        }
        return result;
    }

    private static byte[] parseAddress(String host) {
        if (!host.contains(":")) {
            return ipv4(host);
        }
        String expanded = host;
        if (host.contains(".")) {
            int colon = host.lastIndexOf(':');
            byte[] tail = ipv4(host.substring(colon + 1));
            if (tail == null) {
                throw TargetValidationException.field("url", "INVALID_FORMAT");
            }
            expanded = host.substring(0, colon + 1)
                    + Integer.toHexString((Byte.toUnsignedInt(tail[0]) << 8) | Byte.toUnsignedInt(tail[1]))
                    + ":" + Integer.toHexString((Byte.toUnsignedInt(tail[2]) << 8) | Byte.toUnsignedInt(tail[3]));
        }
        String[] halves = expanded.split("::", -1);
        var left = groups(halves[0]);
        var right = halves.length == 2 ? groups(halves[1]) : new ArrayList<Integer>();
        int zeros = 8 - left.size() - right.size();
        if (halves.length > 2 || zeros < 0 || (halves.length == 1 && zeros != 0)
                || (halves.length == 2 && zeros == 0)) {
            throw TargetValidationException.field("url", "INVALID_FORMAT");
        }
        var all = new ArrayList<>(left);
        for (int i = 0; i < zeros; i++) {
            all.add(0);
        }
        all.addAll(right);
        byte[] result = new byte[16];
        for (int i = 0; i < 8; i++) {
            result[2 * i] = (byte) (all.get(i) >> 8);
            result[2 * i + 1] = (byte) (int) all.get(i);
        }
        boolean mapped = true;
        for (int i = 0; i < 10; i++) {
            mapped &= result[i] == 0;
        }
        if (mapped && result[10] == (byte) 255 && result[11] == (byte) 255) {
            return Arrays.copyOfRange(result, 12, 16);
        }
        return result;
    }

    private static ArrayList<Integer> groups(String value) {
        var result = new ArrayList<Integer>();
        if (!value.isEmpty()) {
            for (String group : value.split(":", -1)) {
                if (!group.matches("[0-9a-f]{1,4}")) {
                    throw TargetValidationException.field("url", "INVALID_FORMAT");
                }
                result.add(Integer.parseInt(group, 16));
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "TargetUrl";
    }
}
