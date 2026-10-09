package com.llmscannerseguridad.domain.project;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectTest {

    private final Instant now = Instant.parse("2026-10-09T10:00:00.123456Z");

    @Test
    void normalizesExteriorWhitespaceAndKeepsInteriorText() {
        var project = project("\u00a0\t Análisis  inicial \u2003", "\n Descripción  libre \t");
        assertThat(project.name()).isEqualTo("Análisis  inicial");
        assertThat(project.description()).isEqualTo("Descripción  libre");
        assertThat(project.updatedAt()).isEqualTo(project.createdAt());
    }

    @Test
    void normalizesEmptyDescriptionsToNull() {
        for (String description : new String[] {null, "", " \t\n\u2003\u00a0"}) {
            assertThat(project("Proyecto", description).description()).isNull();
        }
    }

    @Test
    void rejectsMissingNullAndBlankNames() {
        for (String name : new String[] {null, "", " \t\n\u2003\u00a0"}) {
            assertThatThrownBy(() -> project(name, null))
                    .isInstanceOfSatisfying(ProjectValidationException.class, error ->
                            assertThat(error.fieldErrors()).containsExactly(
                                    new ProjectFieldViolation("name", "REQUIRED", "El nombre es obligatorio.")));
        }
    }

    @Test
    void countsUnicodeCodePointsAfterTrimAtBothLimits() {
        String symbol = "\uD83D\uDD12";
        assertThat(project(" " + symbol.repeat(120) + " ", " " + symbol.repeat(2000) + " ").name())
                .isEqualTo(symbol.repeat(120));
        assertThatThrownBy(() -> project(symbol.repeat(121), null))
                .isInstanceOfSatisfying(ProjectValidationException.class, error ->
                        assertThat(error.fieldErrors().getFirst().code()).isEqualTo("TOO_LONG"));
        assertThatThrownBy(() -> project("Válido", symbol.repeat(2001)))
                .isInstanceOfSatisfying(ProjectValidationException.class, error ->
                        assertThat(error.fieldErrors().getFirst().field()).isEqualTo("description"));
        assertThatThrownBy(() -> project("á".repeat(121), "é".repeat(2001)))
                .isInstanceOfSatisfying(ProjectValidationException.class, error ->
                        assertThat(error.fieldErrors()).hasSize(2));
    }

    @Test
    void rejectsNullCharacterInsideName() {
        assertThatThrownBy(() -> project("Pro\0yecto", null))
                .isInstanceOfSatisfying(ProjectValidationException.class, error ->
                        assertThat(error.fieldErrors()).containsExactly(
                                new ProjectFieldViolation("name", "INVALID_CHARACTER",
                                        "El nombre no admite el carácter U+0000.")));
    }

    @Test
    void rejectsNullCharacterInsideDescription() {
        assertThatThrownBy(() -> project("Proyecto", "Descrip\0ción"))
                .isInstanceOfSatisfying(ProjectValidationException.class, error ->
                        assertThat(error.fieldErrors()).containsExactly(
                                new ProjectFieldViolation("description", "INVALID_CHARACTER",
                                        "La descripción no admite el carácter U+0000.")));
    }

    @Test
    void reportsBothFieldsWhenBothContainNullCharacters() {
        assertThatThrownBy(() -> project("Pro\0yecto", "Descrip\0ción"))
                .isInstanceOfSatisfying(ProjectValidationException.class, error ->
                        assertThat(error.fieldErrors()).containsExactly(
                                new ProjectFieldViolation("name", "INVALID_CHARACTER",
                                        "El nombre no admite el carácter U+0000."),
                                new ProjectFieldViolation("description", "INVALID_CHARACTER",
                                        "La descripción no admite el carácter U+0000.")));
    }

    @Test
    void protectsIdentityAndTemporalInvariants() {
        assertThatThrownBy(() -> new Project(null, "Proyecto", null, now, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Project(UUID.randomUUID(), "Proyecto", null, null, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Project(UUID.randomUUID(), "Proyecto", null, now, now.minusNanos(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Project project(String name, String description) {
        return new Project(UUID.randomUUID(), name, description, now, now);
    }
}
