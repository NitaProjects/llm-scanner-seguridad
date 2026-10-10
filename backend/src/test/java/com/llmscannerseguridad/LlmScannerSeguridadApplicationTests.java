package com.llmscannerseguridad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// El arranque ejecuta Flyway: nunca utilizar el datasource habitual como fallback.
@EnabledIfEnvironmentVariable(named = "SCAN_TEST_DB_URL",
        matches = "jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test")
@SpringBootTest(properties = {
        "spring.datasource.url=${SCAN_TEST_DB_URL}",
        "spring.datasource.username=${SCAN_TEST_DB_USERNAME}",
        "spring.datasource.password=${SCAN_TEST_DB_PASSWORD}"
})
class LlmScannerSeguridadApplicationTests {

    @Test
    void contextLoads() {
    }
}
