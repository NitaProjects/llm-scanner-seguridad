package com.llmscannerseguridad.adapters.out.crypto.target;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Supplier;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.llmscannerseguridad.application.target.CredentialEncryptionPort;
import com.llmscannerseguridad.application.target.CredentialDecryptionPort;
import com.llmscannerseguridad.application.target.EphemeralCredential;
import com.llmscannerseguridad.application.target.TargetOperationException;
import com.llmscannerseguridad.domain.target.EncryptedCredential;

public final class AesGcmCredentialEncryptionAdapter implements CredentialEncryptionPort, CredentialDecryptionPort {
    private final Supplier<String> externalKey;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCredentialEncryptionAdapter(Supplier<String> externalKey) {
        this.externalKey = externalKey;
    }

    @Override
    public EncryptedCredential encrypt(String secret) {
        byte[] key = null;
        byte[] plaintext = null;
        try {
            key = key();
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(new byte[] {CredentialEnvelope.VERSION});
            plaintext = secret.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertextAndTag = cipher.doFinal(plaintext);
            return new EncryptedCredential(CredentialEnvelope.encode(nonce, ciphertextAndTag));
        } catch (GeneralSecurityException | RuntimeException error) {
            // No conservar causas con datos criptográficos ni valores recibidos.
            throw TargetOperationException.internal();
        } finally {
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
    }

    @Override
    public EphemeralCredential decrypt(EncryptedCredential credential) {
        byte[] key = null;
        byte[] plaintext = null;
        try {
            var parts = CredentialEnvelope.decode(credential.bytes());
            key = key();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, parts.nonce()));
            cipher.updateAAD(new byte[] {CredentialEnvelope.VERSION});
            plaintext = cipher.doFinal(parts.ciphertextAndTag());
            return new EphemeralCredential(plaintext);
        } catch (GeneralSecurityException | RuntimeException error) {
            throw TargetOperationException.internal();
        } finally {
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
    }

    private byte[] key() {
        String encoded = externalKey.get();
        if (encoded == null || encoded.isBlank()) {
            throw TargetOperationException.internal();
        }
        byte[] decoded = Base64.getDecoder().decode(encoded);
        if (decoded.length != 32) {
            Arrays.fill(decoded, (byte) 0);
            throw TargetOperationException.internal();
        }
        return decoded;
    }
}
