package com.llmscannerseguridad.application.scan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanStatus;

public interface ScanRepositoryPort {
    // Escrituras dentro de una transacción corta; no representan un commit por sí solas.
    void insert(Scan scan);
    boolean existsActiveByProjectId(UUID projectId);
    Optional<Scan> findById(UUID scanId);
    Optional<Scan> findByProjectIdAndId(UUID projectId, UUID scanId);
    List<Scan> findByProjectIdOrdered(UUID projectId);
    List<Scan> findActive();
    // false indica que otro proceso ganó o el origen ya no está vigente; un fallo lanza excepción.
    boolean transition(ScanStatus expected, Scan scan);
}
