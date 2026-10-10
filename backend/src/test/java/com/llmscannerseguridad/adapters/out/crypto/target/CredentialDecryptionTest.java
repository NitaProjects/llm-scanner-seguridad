package com.llmscannerseguridad.adapters.out.crypto.target;

import java.util.Base64;
import com.llmscannerseguridad.domain.target.EncryptedCredential;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CredentialDecryptionTest {
    private final String key = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void authenticatedRoundTripAndClosedResourceNeverExposeInToString() {
        var crypto = new AesGcmCredentialEncryptionAdapter(() -> key);
        var encrypted = crypto.encrypt("sintético-😀");
        var resource = crypto.decrypt(encrypted);
        assertThat(resource.value()).isEqualTo("sintético-😀");
        assertThat(resource.toString()).doesNotContain("sintético");
        resource.close();
        assertThatThrownBy(resource::value).isInstanceOf(RuntimeException.class);
    }

    @Test
    void missingWrongKeyVersionTruncationAndTagFailWithoutCauseOrPlaintext() {
        var crypto = new AesGcmCredentialEncryptionAdapter(() -> key);
        var encrypted = crypto.encrypt("synthetic-auth");
        for (String invalidKey : new String[] {"", "!", Base64.getEncoder().encodeToString("x".repeat(32).getBytes())}) {
            assertThatThrownBy(() -> new AesGcmCredentialEncryptionAdapter(() -> invalidKey).decrypt(encrypted))
                    .satisfies(error -> {
                        assertThat(error.getCause()).isNull();
                        assertThat(error.getMessage()).doesNotContain("synthetic-auth", invalidKey.isEmpty() ? "unexpected-marker" : invalidKey);
                    });
        }
        for (int position : new int[] {0, 1, encrypted.bytes().length - 1}) {
            byte[] modified = encrypted.bytes();
            modified[position] ^= 127;
            assertThatThrownBy(() -> crypto.decrypt(new EncryptedCredential(modified))).satisfies(error -> assertThat(error.getCause()).isNull());
        }
        assertThatThrownBy(() -> crypto.decrypt(new EncryptedCredential(new byte[] {1}))).isInstanceOf(RuntimeException.class);
    }
}
