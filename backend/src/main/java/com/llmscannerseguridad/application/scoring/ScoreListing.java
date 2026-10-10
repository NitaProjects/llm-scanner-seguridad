package com.llmscannerseguridad.application.scoring;

import java.util.Objects;
import com.llmscannerseguridad.domain.scoring.ScanScore;

public record ScoreListing(ScoreAvailability availability, ScanScore score) {
    public ScoreListing {
        Objects.requireNonNull(availability);
        if ((availability == ScoreAvailability.AVAILABLE) != (score != null)) throw new IllegalArgumentException("Disponibilidad incoherente.");
    }
}
