package com.llmscannerseguridad.demo.config;

import java.net.InetAddress;
import com.llmscannerseguridad.demo.scenario.DemoProfile;

public record DemoSettings(DemoProfile profile, InetAddress address, int port) {
    public static DemoSettings parse(String profile, String bind, String port) {
        try {
            int number = Integer.parseInt(port);
            if (!port.matches("[0-9]{1,5}") || number < 1 || number > 65535) throw new IllegalArgumentException();
            InetAddress address;
            if (bind.contains(":") && bind.matches("[0-9a-fA-F:]+")) {
                address = InetAddress.getByName(bind); // Solo literal IPv6; nunca un nombre DNS.
            } else {
                String[] parts = bind.split("\\.", -1);
                if (parts.length != 4) throw new IllegalArgumentException();
                byte[] bytes = new byte[4];
                for (int i = 0; i < 4; i++) {
                    if (!parts[i].matches("0|[1-9][0-9]{0,2}")) throw new IllegalArgumentException();
                    int octet = Integer.parseInt(parts[i]);
                    if (octet > 255) throw new IllegalArgumentException();
                    bytes[i] = (byte) octet;
                }
                address = InetAddress.getByAddress(bytes);
            }
            return new DemoSettings(DemoProfile.parse(profile), address, number);
        } catch (Exception error) {
            throw new IllegalArgumentException("DEMO_CONFIGURATION_INVALID");
        }
    }

    @Override public String toString() { return "DemoSettings[" + profile + "]"; }
}
