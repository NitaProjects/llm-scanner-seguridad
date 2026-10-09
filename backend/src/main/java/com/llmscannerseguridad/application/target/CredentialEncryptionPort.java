package com.llmscannerseguridad.application.target;

import com.llmscannerseguridad.domain.target.EncryptedCredential;

public interface CredentialEncryptionPort {
    EncryptedCredential encrypt(String secret);
}
