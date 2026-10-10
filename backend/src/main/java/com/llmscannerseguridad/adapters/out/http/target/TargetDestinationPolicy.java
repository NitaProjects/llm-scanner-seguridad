package com.llmscannerseguridad.adapters.out.http.target;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.TargetUrl;

public final class TargetDestinationPolicy {
    // Exclusiones estables; no se consultan registros externos durante ejecución.
    private static final String[] IPV4_SPECIAL = {"0.0.0.0/8", "10.0.0.0/8", "100.64.0.0/10", "127.0.0.0/8",
            "169.254.0.0/16", "172.16.0.0/12", "192.0.0.0/24", "192.0.2.0/24", "192.88.99.0/24",
            "192.31.196.0/24", "192.52.193.0/24", "192.168.0.0/16", "192.175.48.0/24",
            "198.18.0.0/15", "198.51.100.0/24", "203.0.113.0/24", "224.0.0.0/3"};
    private static final String[] IPV6_SPECIAL = {"2001::/23", "2001:db8::/32", "2002::/16",
            "2620:4f:8000::/48", "3ffe::/16", "3fff::/20"};
    private final Set<String> allowed;
    private final Set<String> demo;
    private final Supplier<Set<InetAddress>> interfaces;

    public TargetDestinationPolicy(Set<String> publicOrigins, boolean localProfile, boolean demoEnabled,
            Set<String> demoOrigins) {
        this(publicOrigins, localProfile, demoEnabled, demoOrigins, TargetDestinationPolicy::localAddresses);
    }

    public TargetDestinationPolicy(Set<String> publicOrigins, boolean localProfile, boolean demoEnabled,
            Set<String> demoOrigins, Set<InetAddress> interfaces) {
        this(publicOrigins, localProfile, demoEnabled, demoOrigins, () -> Set.copyOf(interfaces));
    }

    TargetDestinationPolicy(Set<String> publicOrigins, boolean localProfile, boolean demoEnabled,
            Set<String> demoOrigins, Supplier<Set<InetAddress>> interfaces) {
        allowed = normalize(publicOrigins);
        // Se comprueba sintaxis incluso si la excepción está desactivada.
        Set<String> declaredDemo = normalize(demoOrigins);
        demo = localProfile && demoEnabled ? declaredDemo : Set.of();
        this.interfaces = java.util.Objects.requireNonNull(interfaces);
    }

    private static Set<String> normalize(Set<String> entries) {
        var result = new HashSet<String>();
        try {
            for (String entry : entries) {
                var url = new TargetUrl(entry);
                URI uri = URI.create(url.value());
                if (uri.getRawQuery() != null || (uri.getRawPath() != null
                        && !uri.getRawPath().isEmpty() && !uri.getRawPath().equals("/"))) {
                    throw new IllegalArgumentException();
                }
                result.add(url.origin());
            }
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("La lista de orígenes autorizados no es válida.");
        }
        return Set.copyOf(result);
    }

    public boolean authorize(TargetUrl url) {
        if (url.metadataAddress()) {
            throw new InvocationFailure(ExecutionError.TARGET_ADDRESS_BLOCKED);
        }
        if (demo.contains(url.origin())) {
            return true;
        }
        if (!allowed.contains(url.origin())) {
            throw new InvocationFailure(ExecutionError.TARGET_NOT_AUTHORIZED);
        }
        return false;
    }

    public InetAddress select(InetAddress[] addresses, boolean localDemo) {
        if (addresses == null || addresses.length == 0 || addresses.length > 16) {
            throw new InvocationFailure(ExecutionError.DNS_RESOLUTION_FAILED);
        }
        final Set<InetAddress> currentInterfaces;
        try {
            currentInterfaces = Set.copyOf(interfaces.get());
        } catch (RuntimeException error) {
            throw new InvocationFailure(ExecutionError.TARGET_ADDRESS_BLOCKED);
        }
        for (var address : addresses) {
            if (address == null || !permitted(address, localDemo, currentInterfaces)) {
                throw new InvocationFailure(ExecutionError.TARGET_ADDRESS_BLOCKED);
            }
        }
        return addresses[0];
    }

    private boolean permitted(InetAddress address, boolean localDemo, Set<InetAddress> currentInterfaces) {
        byte[] raw = address.getAddress();
        // Normaliza IPv4 mapeada antes de clasificar; nunca permite NAT64/6to4/Teredo.
        if (raw.length == 16 && Arrays.equals(Arrays.copyOf(raw, 12),
                new byte[] {0,0,0,0,0,0,0,0,0,0,(byte) 255,(byte) 255})) {
            raw = Arrays.copyOfRange(raw, 12, 16);
        }
        if (raw.length == 4) {
            if (cidr(raw, "168.63.129.16/32") || cidr(raw, "100.100.100.200/32")) {
                return false;
            }
            boolean privateOrLoopback = cidr(raw, "10.0.0.0/8") || cidr(raw, "172.16.0.0/12")
                    || cidr(raw, "192.168.0.0/16") || cidr(raw, "127.0.0.0/8");
            if (localDemo && privateOrLoopback) {
                return true;
            }
            for (String range : IPV4_SPECIAL) {
                if (cidr(raw, range)) {
                    return false;
                }
            }
        } else {
            if (cidr(raw, "fd00:ec2::254/128")) {
                return false;
            }
            if (localDemo && (cidr(raw, "::1/128") || cidr(raw, "fc00::/7"))) {
                return true;
            }
            if (!cidr(raw, "2000::/3")) {
                return false;
            }
            for (String range : IPV6_SPECIAL) {
                if (cidr(raw, range)) {
                    return false;
                }
            }
        }
        return !currentInterfaces.contains(address) || localDemo;
    }

    private static boolean cidr(byte[] address, String cidr) {
        try {
            String[] parts = cidr.split("/");
            byte[] network = InetAddress.getByName(parts[0]).getAddress(); // Solo literales de la tabla.
            if (network.length != address.length) {
                return false;
            }
            int bits = Integer.parseInt(parts[1]);
            for (int i = 0; i < address.length && bits > 0; i++, bits -= 8) {
                int mask = 0xff << (8 - Math.min(bits, 8));
                if ((address[i] & mask) != (network[i] & mask)) {
                    return false;
                }
            }
            return true;
        } catch (java.net.UnknownHostException error) {
            throw new IllegalStateException("La tabla de redes no es válida.");
        }
    }

    private static Set<InetAddress> localAddresses() {
        try {
            var addresses = new HashSet<InetAddress>();
            for (var network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                addresses.addAll(Collections.list(network.getInetAddresses()));
            }
            return addresses;
        } catch (java.net.SocketException error) {
            throw new IllegalArgumentException("No se ha podido comprobar las interfaces locales.");
        }
    }
}
