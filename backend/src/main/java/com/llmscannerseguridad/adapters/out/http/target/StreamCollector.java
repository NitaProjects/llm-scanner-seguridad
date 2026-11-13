package com.llmscannerseguridad.adapters.out.http.target;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;
import com.llmscannerseguridad.adapters.out.validation.target.TargetJsonProcessor;
import com.llmscannerseguridad.domain.execution.ExecutionError;

final class StreamCollector {
    private static final int FRAME_BYTES = 32768;
    private static final int MAX_UNITS = 1024;
    private final StreamBinding binding;
    private final TargetJsonProcessor json;
    private final int responseBytes;
    private final int frameBytes;
    private final int textBytes;
    private final Runnable deadline;
    private final byte[] buffer = new byte[4096];
    private int cursor, available, bytes, units, frameSize, assembledBytes;
    private boolean skipLf, content, terminal;
    private final StringBuilder assembled = new StringBuilder();

    StreamCollector(StreamBinding binding, TargetJsonProcessor json, ExecutionLimits limits, Runnable deadline) {
        this.binding = binding;
        this.json = json;
        this.responseBytes = limits.responseBytes();
        this.frameBytes = Math.min(FRAME_BYTES, responseBytes);
        this.textBytes = limits.textBytes();
        this.deadline = deadline;
    }

    int bytes() { return bytes; }
    int units() { return units; }
    boolean terminal() { return terminal; }

    String collect(InputStream input) throws IOException {
        var data = new StringBuilder();
        String event = null;
        boolean hasData = false;
        boolean first = true;
        while (true) {
            String line = line(input);
            unit();
            if (first && line.startsWith("\uFEFF") && binding.sse()) line = line.substring(1);
            first = false;
            if (!binding.sse()) {
                if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
                if (!line.isEmpty()) process(null, line);
                frameSize = 0;
            } else if (line.isEmpty()) {
                if (hasData || event != null) process(event == null || event.isEmpty() ? "message" : event, data.toString());
                event = null;
                data.setLength(0);
                hasData = false;
                frameSize = 0;
            } else if (!line.startsWith(":")) {
                int separator = line.indexOf(':');
                String field = separator < 0 ? line : line.substring(0, separator);
                String value = separator < 0 ? "" : line.substring(separator + 1);
                if (value.startsWith(" ")) value = value.substring(1);
                switch (field) {
                    case "data" -> {
                        if (hasData) data.append('\n');
                        data.append(value);
                        hasData = true;
                    }
                    case "event" -> event = value;
                    default -> { }
                }
            }
            if (terminal) {
                if (!content) throw format();
                return assembled.toString();
            }
        }
    }

    private void process(String event, String data) {
        if (binding.errorEvent() != null && binding.errorEvent().equals(event)) throw format();
        if (binding.sse() && (event.equals(binding.terminalEvent()) || data.equals(binding.terminalLiteral()))) {
            terminal = true;
            return;
        }
        if (binding.sse() && binding.contentEvent() != null && !binding.contentEvent().equals(event)) return;
        Object parsed;
        try { parsed = json.parseResponse(data); }
        catch (TargetJsonProcessor.JsonFailure error) { throw format(); }
        if (!(parsed instanceof Map<?, ?>)) throw format();
        if (!binding.sse() && data.equals(binding.terminalLiteral())) { terminal = true; return; }
        if (!binding.sse() && binding.eventPath() != null) {
            if (!(select(parsed, binding.eventPath()) instanceof String type)) throw format();
            if (type.equals(binding.errorEvent())) throw format();
            if (type.equals(binding.terminalEvent())) { terminal = true; return; }
            if (binding.contentEvent() != null && !binding.contentEvent().equals(type)) return;
        }
        if (!(select(parsed, binding.textPath()) instanceof String text)) throw format();
        if (binding.mode() == StreamBinding.Mode.SNAPSHOT) {
            assembled.setLength(0);
            assembledBytes = 0;
        }
        assembledBytes += text.getBytes(StandardCharsets.UTF_8).length;
        if (assembledBytes > textBytes) throw new InvocationFailure(ExecutionError.EVIDENCE_LIMIT_EXCEEDED);
        assembled.append(text);
        content = true;
    }

    private static Object select(Object value, List<Object> path) {
        for (Object segment : path) {
            if (segment instanceof String key && value instanceof Map<?, ?> map) value = map.get(key);
            else if (segment instanceof Integer index && value instanceof List<?> list && index < list.size()) value = list.get(index);
            else return null;
        }
        return value;
    }

    private String line(InputStream input) throws IOException {
        var line = new ByteArrayOutputStream();
        for (;;) {
            int value = read(input);
            if (value == -1) throw format(); // EOF nunca delimita un terminal.
            if (skipLf) {
                skipLf = false;
                if (value == '\n') {
                    if (frameSize > 0 && ++frameSize > frameBytes) throw tooLarge();
                    continue;
                }
            }
            if (++frameSize > frameBytes) throw tooLarge();
            if (value == '\n' || value == '\r' && binding.sse()) {
                skipLf = value == '\r';
                break;
            }
            line.write(value);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(line.toByteArray())).toString();
        } catch (CharacterCodingException error) { throw format(); }
    }

    private int read(InputStream input) throws IOException {
        if (cursor == available) {
            deadline.run();
            available = input.read(buffer, 0, Math.min(buffer.length, responseBytes - bytes + 1));
            cursor = 0;
            if (available < 0) return -1;
        }
        if (++bytes > responseBytes) throw tooLarge();
        return Byte.toUnsignedInt(buffer[cursor++]);
    }

    private void unit() {
        deadline.run();
        if (++units > Math.min(MAX_UNITS, responseBytes)) throw tooLarge();
    }

    private static InvocationFailure tooLarge() { return new InvocationFailure(ExecutionError.RESPONSE_TOO_LARGE); }
    private static InvocationFailure format() { return new InvocationFailure(ExecutionError.RESPONSE_FORMAT_ERROR); }
}
