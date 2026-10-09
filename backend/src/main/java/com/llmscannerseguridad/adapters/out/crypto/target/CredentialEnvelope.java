package com.llmscannerseguridad.adapters.out.crypto.target;

import java.nio.ByteBuffer;
import java.util.Arrays;
import com.llmscannerseguridad.application.target.TargetOperationException;

// Layout v1: versión (1 byte), nonce (12 bytes), ciphertext y tag GCM final (16 bytes).
final class CredentialEnvelope {
    static final byte VERSION = 1;

    private CredentialEnvelope() {
    }

    static byte[] encode(byte[] nonce, byte[] ciphertextAndTag) {
        if (nonce.length != 12 || ciphertextAndTag.length < 16) {
            throw TargetOperationException.internal();
        }
        return ByteBuffer.allocate(13 + ciphertextAndTag.length)
                .put(VERSION).put(nonce).put(ciphertextAndTag).array();
    }

    static Parts decode(byte[] material) {
        if (material == null || material.length < 29 || material[0] != VERSION) {
            throw TargetOperationException.internal();
        }
        return new Parts(Arrays.copyOfRange(material, 1, 13), Arrays.copyOfRange(material, 13, material.length));
    }

    record Parts(byte[] nonce, byte[] ciphertextAndTag) {
        @Override
        public String toString() {
            return "CredentialEnvelope[protegida]";
        }
    }
}
