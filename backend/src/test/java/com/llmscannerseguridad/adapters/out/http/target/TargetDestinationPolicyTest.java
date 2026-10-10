package com.llmscannerseguridad.adapters.out.http.target;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.TargetUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class TargetDestinationPolicyTest {
    private final TargetDestinationPolicy policy = new TargetDestinationPolicy(Set.of("https://target.example"),
            false, false, Set.of(), Set.of());

    @ParameterizedTest
    @ValueSource(strings = {"0.1.2.3", "10.0.0.1", "100.64.0.1", "100.100.100.200", "127.0.0.1",
            "169.254.169.254", "169.254.170.2", "172.16.0.1", "192.168.0.1", "192.0.2.1",
            "198.18.0.1", "198.51.100.1", "203.0.113.1", "224.0.0.1", "255.255.255.255",
            "168.63.129.16", "::", "::1", "fe80::1", "fc00::1", "ff02::1", "2001:db8::1",
            "fd00:ec2::254", "::ffff:127.0.0.1", "64:ff9b::7f00:1", "2002:7f00:1::"})
    void rejectsSpecialAddressesAndMixedAnswers(String literal) throws Exception {
        var forbidden = InetAddress.getByName(literal);
        var publicAddress = InetAddress.getByName("8.8.8.8"); // Literal sintético para clasificación; nunca se conecta.
        assertThatThrownBy(() -> policy.select(new InetAddress[] {publicAddress, forbidden}, false))
                .isInstanceOfSatisfying(InvocationFailure.class,
                        error -> assertThat(error.reason).isEqualTo(ExecutionError.TARGET_ADDRESS_BLOCKED));
    }

    @Test
    void exactOriginNormalizationEmptyAllowlistAndControlledDemo() throws Exception {
        assertThat(policy.authorize(new TargetUrl("HTTPS://TARGET.EXAMPLE:443/path?q=1"))).isFalse();
        assertThatThrownBy(() -> policy.authorize(new TargetUrl("https://other.example"))).isInstanceOf(InvocationFailure.class);
        assertThatThrownBy(() -> new TargetDestinationPolicy(Set.of("https://*.example"), false, false, Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.select(new InetAddress[17], false)).isInstanceOf(InvocationFailure.class);
        var local = new TargetUrl("http://127.0.0.1:12345");
        for (boolean profile : new boolean[] {false, true}) {
            var demo = new TargetDestinationPolicy(Set.of(), profile, true, Set.of(local.value()), Set.of());
            if (profile) {
                assertThat(demo.authorize(local)).isTrue();
                assertThat(demo.select(new InetAddress[] {InetAddress.getByName("127.0.0.1")}, true)).isNotNull();
                for (String denied : new String[] {"169.254.169.254", "100.100.100.200", "168.63.129.16", "fd00:ec2::254", "fe80::1"}) {
                    assertThatThrownBy(() -> demo.select(new InetAddress[] {InetAddress.getByName(denied)}, true))
                            .isInstanceOf(InvocationFailure.class);
                }
            } else {
                assertThatThrownBy(() -> demo.authorize(local)).isInstanceOf(InvocationFailure.class);
            }
        }
        var self = InetAddress.getByName("8.8.8.8");
        var ownInterface = new TargetDestinationPolicy(Set.of(), false, false, Set.of(), Set.of(self));
        assertThatThrownBy(() -> ownInterface.select(new InetAddress[] {self}, false)).isInstanceOf(InvocationFailure.class);
    }

    @Test
    void eachSelectionRefreshesLocalInterfacesAndRejectsNewAddressIncludingMixedDns() throws Exception {
        var a = InetAddress.getByName("8.8.8.8");
        for (String literal : new String[] {"1.1.1.1", "2606:4700:4700::1111"}) {
            var b = InetAddress.getByName(literal);
            var current = new AtomicReference<>(Set.of(a));
            var reads = new AtomicInteger();
            var refreshed = new TargetDestinationPolicy(Set.of("https://target.example"), false, false, Set.of(), () -> {
                reads.incrementAndGet();
                return current.get();
            });
            assertThat(refreshed.select(new InetAddress[] {b}, false)).isEqualTo(b);
            assertThat(reads).hasValue(1);
            current.set(Set.of(a, b));
            assertThatThrownBy(() -> refreshed.select(new InetAddress[] {b}, false))
                    .isInstanceOfSatisfying(InvocationFailure.class,
                            error -> assertThat(error.reason).isEqualTo(ExecutionError.TARGET_ADDRESS_BLOCKED));
            assertThat(reads).hasValue(2);
            var other = InetAddress.getByName("9.9.9.9");
            assertThatThrownBy(() -> refreshed.select(new InetAddress[] {other, b}, false)).isInstanceOf(InvocationFailure.class);
            assertThat(refreshed.select(new InetAddress[] {other}, false)).isEqualTo(other);
        }
    }

    @Test
    void unavailableInterfacesFailClosedEvenForControlledDemo() throws Exception {
        var local = new TargetUrl("http://127.0.0.1:12345");
        var unavailable = new TargetDestinationPolicy(Set.of("https://target.example"), true, true, Set.of(local.value()),
                () -> { throw new IllegalStateException("fixture interface enumeration failure"); });
        assertThat(unavailable.authorize(local)).isTrue();
        for (boolean demo : new boolean[] {false, true}) {
            assertThatThrownBy(() -> unavailable.select(new InetAddress[] {InetAddress.getByName(demo ? "127.0.0.1" : "8.8.8.8")}, demo))
                    .isInstanceOfSatisfying(InvocationFailure.class, error -> {
                        assertThat(error.reason).isEqualTo(ExecutionError.TARGET_ADDRESS_BLOCKED);
                        assertThat(error.getCause()).isNull();
                        assertThat(error.getMessage()).doesNotContain("fixture");
                    });
        }
    }

    @Test
    void refreshedInterfacesKeepDemoRestrictedToExplicitOriginProfileAndFlag() throws Exception {
        var local = new TargetUrl("http://127.0.0.1:12345");
        var loopback = InetAddress.getByName("127.0.0.1");
        for (boolean profile : new boolean[] {false, true}) {
            for (boolean enabled : new boolean[] {false, true}) {
                var demo = new TargetDestinationPolicy(Set.of(), profile, enabled, Set.of(local.value()), () -> Set.of(loopback));
                if (profile && enabled) {
                    assertThat(demo.select(new InetAddress[] {loopback}, demo.authorize(local))).isEqualTo(loopback);
                    assertThatThrownBy(() -> demo.authorize(new TargetUrl("http://127.0.0.1:12346"))).isInstanceOf(InvocationFailure.class);
                } else {
                    assertThatThrownBy(() -> demo.authorize(local)).isInstanceOf(InvocationFailure.class);
                }
            }
        }
    }

    @Test
    void boundedDnsTimesOutSaturatesAndNeverContinuesToConnection() throws Exception {
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        try (var resolver = new BoundedDnsResolver(host -> {
            calls.incrementAndGet();
            boolean done = false;
            while (!done) {
                try { done = release.await(10, TimeUnit.MILLISECONDS); }
                catch (InterruptedException ignored) { /* Simula DNS de sistema no interrumpible. */ }
            }
            return new InetAddress[] {InetAddress.getLoopbackAddress()};
        })) {
            try {
                for (int i = 0; i < 12; i++) {
                    assertThatThrownBy(() -> resolver.resolve("controlled.invalid", TimeUnit.MILLISECONDS.toNanos(10)))
                            .isInstanceOf(InvocationFailure.class);
                }
                assertThat(calls.get()).isLessThanOrEqualTo(2);
            } finally {
                release.countDown();
            }
        }
    }
}
