package com.llmscannerseguridad.adapters.out.validation.target;

import java.util.List;
import java.util.Map;
import com.llmscannerseguridad.application.target.ConfigurationValidationPort;
import com.llmscannerseguridad.domain.target.TargetRules;
import com.llmscannerseguridad.domain.target.TargetSettings;
import com.llmscannerseguridad.domain.target.TargetValidationException;
import tools.jackson.core.JacksonException;

public final class JsonTargetConfigurationValidator implements ConfigurationValidationPort {
    private final StrictTargetJson json = new StrictTargetJson();
    private final LocalTargetUrlPolicy urls;

    public JsonTargetConfigurationValidator(LocalTargetUrlPolicy urls) {
        this.urls = urls;
    }

    @Override
    public void validate(TargetSettings settings) {
        urls.validate(settings.url());
        template(settings.requestTemplate());
        responsePath(settings.responsePath());
    }

    public void template(String value) {
        TargetRules.noNullCharacter(value, "requestTemplate");
        if (value == null || value.isBlank()) {
            throw TargetValidationException.field("requestTemplate", "REQUIRED");
        }
        if (TargetRules.bytes(value) > 32768) {
            throw TargetValidationException.field("requestTemplate", "TOO_LONG");
        }
        Object document;
        try {
            document = json.read(value);
        } catch (JacksonException error) {
            throw TargetValidationException.field("requestTemplate", "INVALID_FORMAT");
        }
        if (!(document instanceof Map<?, ?>) || countTokens(document) != 1) {
            throw TargetValidationException.field("requestTemplate", "INVALID_FORMAT");
        }
    }

    private int countTokens(Object value) {
        int count = 0;
        if (value instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                String key = (String) entry.getKey();
                TargetRules.noNullCharacter(key, "requestTemplate");
                if (key.contains(TargetSettings.PROMPT_PLACEHOLDER)) {
                    throw TargetValidationException.field("requestTemplate", "INVALID_FORMAT");
                }
                count += countTokens(entry.getValue());
            }
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                count += countTokens(item);
            }
        } else if (value instanceof String text) {
            TargetRules.noNullCharacter(text, "requestTemplate");
            if (text.contains(TargetSettings.PROMPT_PLACEHOLDER)) {
                if (!text.equals(TargetSettings.PROMPT_PLACEHOLDER)) {
                    throw TargetValidationException.field("requestTemplate", "INVALID_FORMAT");
                }
                count++;
            }
        }
        return count;
    }

    public void responsePath(String raw) {
        pathSegments(raw);
    }

    public List<Object> pathSegments(String raw) {
        var result = new java.util.ArrayList<Object>();
        TargetRules.noNullCharacter(raw, "responsePath");
        String path = TargetRules.trim(raw);
        if (path == null || path.isEmpty()) {
            throw TargetValidationException.field("responsePath", "REQUIRED");
        }
        TargetRules.length(path, 1024, "responsePath");
        if (path.charAt(0) != '$') {
            invalidPath();
        }
        int offset = 1;
        int segments = 0;
        while (offset < path.length()) {
            if (++segments > 32) {
                invalidPath();
            }
            char next = path.charAt(offset++);
            if (next == '.') {
                int start = offset;
                if (offset >= path.length() || !identifierStart(path.charAt(offset))) {
                    invalidPath();
                }
                offset++;
                while (offset < path.length() && (identifierStart(path.charAt(offset))
                        || (path.charAt(offset) >= '0' && path.charAt(offset) <= '9'))) {
                    offset++;
                }
                if (offset == start) {
                    invalidPath();
                }
                result.add(path.substring(start, offset));
            } else if (next == '[') {
                if (offset < path.length() && path.charAt(offset) == '"') {
                    int start = offset++;
                    boolean escaped = false;
                    boolean closed = false;
                    while (offset < path.length()) {
                        char ch = path.charAt(offset++);
                        if (escaped) {
                            escaped = false;
                        } else if (ch == '\\') {
                            escaped = true;
                        } else if (ch == '"') {
                            closed = true;
                            break;
                        }
                    }
                    if (!closed) {
                        invalidPath();
                    }
                    try {
                        Object property = json.read(path.substring(start, offset));
                        if (!(property instanceof String text) || text.isEmpty()) {
                            invalidPath();
                        } else {
                            TargetRules.noNullCharacter(text, "responsePath");
                            result.add(text);
                        }
                    } catch (JacksonException error) {
                        invalidPath();
                    }
                } else {
                    int start = offset;
                    while (offset < path.length() && path.charAt(offset) >= '0' && path.charAt(offset) <= '9') {
                        offset++;
                    }
                    String index = path.substring(start, offset);
                    if (!index.matches("0|[1-9][0-9]{0,9}")) {
                        invalidPath();
                    }
                    if (Long.parseLong(index) > Integer.MAX_VALUE) {
                        invalidPath();
                    }
                    result.add(Integer.parseInt(index));
                }
                if (offset >= path.length() || path.charAt(offset++) != ']') {
                    invalidPath();
                }
            } else {
                invalidPath();
            }
        }
        return List.copyOf(result);
    }

    private static boolean identifierStart(char value) {
        return value == '_' || (value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z');
    }

    private static void invalidPath() {
        throw TargetValidationException.field("responsePath", "INVALID_FORMAT");
    }
}
