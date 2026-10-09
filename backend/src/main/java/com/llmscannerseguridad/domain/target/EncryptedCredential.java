package com.llmscannerseguridad.domain.target;

import java.util.Objects;

// Material opaco e inmutable; nunca forma parte del contrato REST.
public final class EncryptedCredential {
    private final byte[] envelope;

    public EncryptedCredential(byte[] envelope) {
        this.envelope = Objects.requireNonNull(envelope).clone();
        if (envelope.length == 0) {
            throw new IllegalArgumentException("Material cifrado no válido.");
        }
    }

    public byte[] bytes() {
        return envelope.clone();
    }

    @Override
    public String toString() {
        return "EncryptedCredential[protegida]";
    }
}
