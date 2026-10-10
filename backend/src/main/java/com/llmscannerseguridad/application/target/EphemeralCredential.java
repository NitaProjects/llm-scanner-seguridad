package com.llmscannerseguridad.application.target;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// Recurso interno; nunca forma parte de un resultado de aplicación o persistencia.
public final class EphemeralCredential implements AutoCloseable {
    private final byte[] plaintext;
    private boolean closed;

    public EphemeralCredential(byte[] plaintext) {
        this.plaintext = plaintext.clone();
    }

    public String value() {
        if (closed) {
            throw TargetOperationException.internal();
        }
        java.nio.CharBuffer decoded = null;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(plaintext));
            return decoded.toString();
        } catch (java.nio.charset.CharacterCodingException error) {
            throw TargetOperationException.internal();
        } finally {
            if (decoded != null && decoded.hasArray()) {
                Arrays.fill(decoded.array(), '\0');
            }
        }
    }

    @Override
    public void close() {
        Arrays.fill(plaintext, (byte) 0);
        closed = true;
    }

    @Override
    public String toString() {
        return "EphemeralCredential[protegida]";
    }
}
