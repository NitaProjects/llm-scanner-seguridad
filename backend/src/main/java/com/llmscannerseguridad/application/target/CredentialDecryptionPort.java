package com.llmscannerseguridad.application.target;

import com.llmscannerseguridad.domain.target.EncryptedCredential;

public interface CredentialDecryptionPort {
    EphemeralCredential decrypt(EncryptedCredential credential);
}
