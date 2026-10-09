package com.llmscannerseguridad.adapters.in.rest.project;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.llmscannerseguridad.domain.project.ProjectFieldViolation;
import com.llmscannerseguridad.domain.project.ProjectValidationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

public final class ProjectRequestMapper {

    private static final Set<String> FIELDS = Set.of("name", "description");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final ObjectReader reader;

    public ProjectRequestMapper(JsonMapper mapper) {
        reader = mapper.readerFor(Object.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    public CreateProjectRequest creation(String json) {
        if (json == null || json.isBlank()) {
            throw new InvalidProjectRequestException();
        }
        Object body;
        try {
            body = reader.readValue(json);
        } catch (JacksonException error) {
            throw new InvalidProjectRequestException();
        }
        if (!(body instanceof Map<?, ?> fields) || !FIELDS.containsAll(fields.keySet())) {
            throw new InvalidProjectRequestException();
        }
        Object name = fields.get("name");
        Object description = fields.get("description");
        var errors = new ArrayList<ProjectFieldViolation>();
        if (name != null && !(name instanceof String)) {
            errors.add(new ProjectFieldViolation("name", "INVALID_TYPE", "El nombre debe ser texto."));
        }
        if (description != null && !(description instanceof String)) {
            errors.add(new ProjectFieldViolation("description", "INVALID_TYPE", "La descripción debe ser texto o null."));
        }
        if (!errors.isEmpty()) {
            throw new ProjectValidationException(errors);
        }
        return new CreateProjectRequest((String) name, (String) description);
    }

    public UUID identifier(String value) {
        if (value == null || !UUID_PATTERN.matcher(value).matches()) {
            throw new InvalidProjectIdException();
        }
        return UUID.fromString(value);
    }
}
