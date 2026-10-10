package com.llmscannerseguridad.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.llmscannerseguridad.LlmScannerSeguridadApplication;
import com.llmscannerseguridad.application.scan.ScanWorkPort;
import org.flywaydb.core.Flyway;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

// Solo se usa con PostgreSQL dedicado y autorización explícita para tests y migraciones.
final class ScanTestDatabase implements AutoCloseable {
    final String schema = "scan_test_" + UUID.randomUUID().toString().replace("-", "");
    private final String url = Objects.requireNonNull(System.getenv("SCAN_TEST_DB_URL"));
    private final String username = Objects.requireNonNull(System.getenv("SCAN_TEST_DB_USERNAME"),
            "Configura SCAN_TEST_DB_USERNAME para la base dedicada.");
    private final String password = Objects.requireNonNull(System.getenv("SCAN_TEST_DB_PASSWORD"),
            "Configura SCAN_TEST_DB_PASSWORD externamente, sin incluirlo en el repositorio.");
    private final List<ConfigurableApplicationContext> applications = new ArrayList<>();

    void migrate(String target) {
        var configuration = Flyway.configure().dataSource(url, username, password).schemas(schema)
                .defaultSchema(schema).createSchemas(true).locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        configuration.load().migrate();
    }

    ConfigurableApplicationContext open() {
        return open(null);
    }

    ConfigurableApplicationContext open(ScanWorkPort work) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", url);
        properties.put("spring.datasource.username", username);
        properties.put("spring.datasource.password", password);
        properties.put("spring.datasource.hikari.schema", schema);
        properties.put("spring.datasource.hikari.maximum-pool-size", 8);
        properties.put("spring.jpa.properties.hibernate.default_schema", schema);
        properties.put("spring.jpa.hibernate.ddl-auto", "validate");
        properties.put("spring.jpa.open-in-view", false);
        properties.put("spring.flyway.schemas", schema);
        properties.put("spring.flyway.default-schema", schema);
        properties.put("spring.flyway.create-schemas", true);
        properties.put("spring.flyway.locations", "classpath:db/migration");
        properties.put("spring.flyway.enabled", true);
        properties.put("spring.sql.init.mode", "never");
        properties.put("TARGET_CREDENTIAL_ENCRYPTION_KEY", "");
        properties.put("app.scan.executor.core-pool-size", 1);
        properties.put("app.scan.executor.max-pool-size", 1);
        properties.put("app.scan.executor.queue-capacity", 10);
        var context = new SpringApplicationBuilder(LlmScannerSeguridadApplication.class)
                .web(WebApplicationType.NONE).registerShutdownHook(false)
                .initializers(application -> {
                    application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("scan-execution-test", properties));
                    if (work != null) {
                        ((GenericApplicationContext) application).registerBean("scanTestWork", ScanWorkPort.class,
                                () -> work, definition -> definition.setPrimary(true));
                    }
                }).run();
        applications.add(context);
        return context;
    }

    void closeApplication(ConfigurableApplicationContext application) {
        application.close();
        applications.remove(application);
    }

    Connection connection() throws SQLException {
        var connection = DriverManager.getConnection(url, username, password);
        try {
            connection.setSchema(schema);
            return connection;
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
    }

    JdbcTemplate jdbc() {
        return new JdbcTemplate(new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return connection();
            }

            @Override
            public Connection getConnection(String ignoredUser, String ignoredPassword) throws SQLException {
                return connection();
            }
        });
    }

    @Override
    public void close() throws Exception {
        for (var application : applications) {
            application.close();
        }
        if (!schema.matches("scan_test_[0-9a-f]{32}")) {
            throw new IllegalStateException("Esquema de pruebas no válido.");
        }
        // Solo elimina el esquema aleatorio de esta fixture; nunca tablas compartidas.
        try (var connection = DriverManager.getConnection(url, username, password);
                var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }
}
