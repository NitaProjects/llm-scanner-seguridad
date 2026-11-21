package com.llmscannerseguridad.integration;

import java.sql.Connection;
import java.util.*;
import java.util.regex.Pattern;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

/** Oráculo de fixture: no importa ni llama al inspector/normalizador de producción. */
final class IndependentPrivacyOracle {
    private static final Pattern UNICODE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Set<String> TABLES = Set.of("test_executions", "evaluation_results", "findings",
            "scan_scores", "security_reports", "scans", "projects", "test_cases", "target_configurations");

    static void audit(Connection connection, Collection<String> sentinels) throws Exception {
        assertThat(connection.getAutoCommit()).isTrue();
        for (String table : TABLES) {
            try (var statement = connection.createStatement();
                    var rows = statement.executeQuery("select (to_jsonb(r)-'credential_ciphertext')::text from " + table + " r")) {
                while (rows.next()) {
                    assertThat(absent(rows.getString(1), sentinels)).as("Auditoría independiente: %s", table).isTrue();
                }
            }
        }
    }

    static boolean absent(Object root, Collection<String> sentinels) {
        var pending = new ArrayDeque<Object>(); pending.add(root);
        var visited = new HashSet<String>(); int nodes = 0;
        while (!pending.isEmpty()) {
            if (++nodes > 32768) throw new IllegalStateException("Fixture del oráculo excedida.");
            Object value = pending.pop();
            if (value instanceof Map<?, ?> map) {
                map.forEach((k, v) -> { pending.add(k); if (v != null) pending.add(v); }); continue;
            }
            if (value instanceof List<?> list) { list.stream().filter(Objects::nonNull).forEach(pending::add); continue; }
            if (value == null) continue;
            String text = value.toString(); if (!visited.add(text)) continue;
            for (String sentinel : sentinels) if (text.contains(sentinel)) return false;
            try { Object json = JsonMapper.shared().readValue(text, Object.class); if (json != null) pending.add(json); }
            catch (RuntimeException ordinaryText) { /* También se inspecciona Unicode incrustado en prosa. */ }
            var matcher = UNICODE.matcher(text); var decoded = new StringBuilder();
            while (matcher.find()) matcher.appendReplacement(decoded, java.util.regex.Matcher.quoteReplacement(
                    Character.toString((char) Integer.parseInt(matcher.group(1), 16))));
            matcher.appendTail(decoded);
            String next = decoded.toString().replace("\\\"", "\"").replace("\\/", "/")
                    .replace("\\b", "\b").replace("\\f", "\f").replace("\\n", "\n")
                    .replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
            if (!next.equals(text)) pending.add(next);
        }
        return true;
    }
    private IndependentPrivacyOracle() { }
}
