package com.llmscannerseguridad.adapters.out.crypto.target;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.llmscannerseguridad.application.target.TargetOperationException;
import com.llmscannerseguridad.domain.target.AuthType;
import com.llmscannerseguridad.domain.target.TargetRules;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AesGcmCredentialEncryptionAdapterTest {
    @Test
    void realAesUsesNewNonceAndRecoversAfterNewAdapterWithStableSyntheticKey() throws Exception {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String encoded = Base64.getEncoder().encodeToString(key);
        String marker = " sintético-" + java.util.UUID.randomUUID() + " ";
        var adapter = new AesGcmCredentialEncryptionAdapter(() -> encoded);
        byte[] first = adapter.encrypt(marker).bytes();
        byte[] second = new AesGcmCredentialEncryptionAdapter(() -> encoded).encrypt(marker).bytes();
        assertThat(first[0]).isEqualTo((byte) 1);
        assertThat(Arrays.copyOfRange(first, 1, 13)).isNotEqualTo(Arrays.copyOfRange(second, 1, 13));
        assertThat(new String(first, StandardCharsets.UTF_8)).doesNotContain(marker);
        assertThat(decrypt(first, key)).isEqualTo(marker);
        assertThat(decrypt(second, key)).isEqualTo(marker);
        byte[] wrong = key.clone();
        wrong[0] ^= 1;
        assertThatThrownBy(() -> decrypt(first, wrong)).isInstanceOf(GeneralSecurityException.class);
        byte[] changed = first.clone();
        changed[changed.length - 1] ^= 1;
        assertThatThrownBy(() -> decrypt(changed, key)).isInstanceOf(GeneralSecurityException.class);
        changed[0] = 2;
        assertThatThrownBy(() -> decrypt(changed, key)).isInstanceOf(TargetOperationException.class);
        assertThatThrownBy(() -> CredentialEnvelope.decode(new byte[28])).isInstanceOf(TargetOperationException.class);
    }

    @Test
    void validSupplementarySecretIsRecoveredExactlyWithoutNormalization() throws Exception {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String encoded = Base64.getEncoder().encodeToString(key);
        String marker = " sintético-\uD83D\uDD12 ";
        TargetRules.secret(marker, AuthType.API_KEY_HEADER);
        byte[] encrypted = new AesGcmCredentialEncryptionAdapter(() -> encoded).encrypt(marker).bytes();
        assertThat(decrypt(encrypted, key)).isEqualTo(marker);
    }

    @Test
    void absentInvalidOrUnavailableKeyFailsWithSanitizedErrorAndNoDefault() {
        for (String value : new String[] {"", "invalid-base64", Base64.getEncoder().encodeToString(new byte[16])}) {
            assertThatThrownBy(() -> new AesGcmCredentialEncryptionAdapter(() -> value).encrypt("sintético"))
                    .isInstanceOfSatisfying(TargetOperationException.class, error -> {
                        assertThat(error.reason()).isEqualTo(TargetOperationException.Reason.INTERNAL_ERROR);
                        assertThat(error.getCause()).isNull();
                        assertThat(error.getMessage()).doesNotContain(value.isEmpty() ? "AES" : value);
                    });
        }
        var reads = new AtomicInteger();
        var absent = new AesGcmCredentialEncryptionAdapter(() -> { reads.incrementAndGet(); return null; });
        assertThat(reads).hasValue(0);
        assertThatThrownBy(() -> absent.encrypt("sintético")).isInstanceOf(TargetOperationException.class);
        assertThat(reads).hasValue(1);
        assertThatThrownBy(() -> new AesGcmCredentialEncryptionAdapter(() -> {
            throw new IllegalStateException("dato interno sintético");
        }).encrypt("sintético")).hasMessage("No se ha podido completar la operación de target.");
    }

    // Verificación del envelope mediante JCA real solo en pruebas, nunca desde GET/PUT.
    private String decrypt(byte[] envelope, byte[] key) throws Exception {
        var parts = CredentialEnvelope.decode(envelope);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, parts.nonce()));
        cipher.updateAAD(new byte[] {CredentialEnvelope.VERSION});
        return new String(cipher.doFinal(parts.ciphertextAndTag()), StandardCharsets.UTF_8);
    }
}
