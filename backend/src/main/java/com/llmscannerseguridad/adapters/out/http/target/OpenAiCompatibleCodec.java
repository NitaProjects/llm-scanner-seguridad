package com.llmscannerseguridad.adapters.out.http.target;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.llmscannerseguridad.adapters.out.validation.target.TargetJsonProcessor;
import com.llmscannerseguridad.domain.target.TargetSettings;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import tools.jackson.databind.json.JsonMapper;

public final class OpenAiCompatibleCodec {
    private final JsonMapper json = JsonMapper.builder().build();
    private final TargetJsonProcessor responses = new TargetJsonProcessor(null);

    public String render(TargetSettings settings, String prompt) {
        if (settings.adapterType() != com.llmscannerseguridad.domain.target.TargetAdapterType.OPENAI_COMPATIBLE) {
            throw new IllegalArgumentException("Configuración chat no válida.");
        }
        if (SecurityTest.invalidUnicode(prompt)) throw new IllegalArgumentException("Prompt no válido.");
        var messages = new ArrayList<Map<String, String>>();
        if (settings.systemPrompt() != null && !settings.systemPrompt().isEmpty()) {
            messages.add(message("system", settings.systemPrompt()));
        }
        messages.add(message("user", prompt));
        var body = new LinkedHashMap<String, Object>();
        body.put("model", settings.model());body.put("messages", messages);body.put("stream", false);
        return json.writeValueAsString(body);
    }

    private Map<String, String> message(String role, String content) {
        var result = new LinkedHashMap<String, String>();
        result.put("role", role);result.put("content", content);return result;
    }

    public String extract(String body) {
        Object parsed = responses.parseResponse(body);
        if (!(parsed instanceof Map<?, ?> root)) throw failure(ExecutionError.RESPONSE_FORMAT_ERROR);
        if (!root.containsKey("choices")) throw failure(ExecutionError.RESPONSE_PATH_NOT_FOUND);
        if (!(root.get("choices") instanceof List<?> choices)) throw failure(ExecutionError.RESPONSE_FORMAT_ERROR);
        if (choices.isEmpty()) throw failure(ExecutionError.RESPONSE_PATH_NOT_FOUND);
        if (choices.size() != 1 || !(choices.getFirst() instanceof Map<?, ?> choice)) throw failure(ExecutionError.RESPONSE_FORMAT_ERROR);
        if (!choice.containsKey("message")) throw failure(ExecutionError.RESPONSE_PATH_NOT_FOUND);
        if (!(choice.get("message") instanceof Map<?, ?> message)) throw failure(ExecutionError.RESPONSE_FORMAT_ERROR);
        if (!message.containsKey("content")) throw failure(ExecutionError.RESPONSE_PATH_NOT_FOUND);
        if (!(message.get("content") instanceof String content)) throw failure(ExecutionError.RESPONSE_NOT_TEXT);
        if (!"stop".equals(choice.get("finish_reason"))
                || choice.containsKey("index") && (!(choice.get("index") instanceof Number n)
                    || new java.math.BigDecimal(n.toString()).compareTo(java.math.BigDecimal.ZERO) != 0)
                || message.containsKey("role") && !"assistant".equals(message.get("role"))
                || nonempty(message.get("tool_calls")) || nonempty(message.get("function_call"))) {
            throw failure(ExecutionError.RESPONSE_FORMAT_ERROR);
        }
        return content;
    }

    private boolean nonempty(Object value) {
        return value != null && !(value instanceof List<?> list && list.isEmpty())
                && !(value instanceof Map<?, ?> map && map.isEmpty());
    }
    private TargetJsonProcessor.JsonFailure failure(ExecutionError error) { return new TargetJsonProcessor.JsonFailure(error); }
}
