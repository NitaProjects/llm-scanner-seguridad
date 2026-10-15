package com.llmscannerseguridad.demo.config;

import com.llmscannerseguridad.demo.http.AdmissionGate;
import com.llmscannerseguridad.demo.http.DemoHttpFilter;
import com.llmscannerseguridad.demo.scenario.ScenarioCatalog;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.catalina.core.StandardHost;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class DemoConfiguration {
    @Bean
    DemoSettings demoSettings(Environment environment) {
        return DemoSettings.parse(environment.getProperty("DEMO_PROFILE", "SAFE"),
                environment.getProperty("DEMO_BIND_ADDRESS", "127.0.0.1"),
                environment.getProperty("DEMO_PORT", "8081"));
    }

    @Bean
    ScenarioCatalog scenarioCatalog(DemoSettings settings) {
        return new ScenarioCatalog(settings.profile(), CatalogLoader.load());
    }

    @Bean
    FilterRegistrationBean<DemoHttpFilter> demoHttp(ScenarioCatalog catalog, DemoSettings settings) {
        var registration = new FilterRegistrationBean<>(new DemoHttpFilter(catalog, settings.profile(),
                new AdmissionGate(System::nanoTime)));
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(jakarta.servlet.DispatcherType.REQUEST, jakarta.servlet.DispatcherType.ERROR);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> transportLimits(DemoSettings settings) {
        return factory -> {
            factory.setAddress(settings.address());
            factory.addConnectorCustomizers(connector -> {
                var protocol = (AbstractHttp11Protocol<?>) connector.getProtocolHandler();
                protocol.setMaxHttpRequestHeaderSize(8192);
                protocol.setMaxConnections(16);
                protocol.setMaxThreads(4);
                protocol.setMinSpareThreads(1);
                protocol.setAcceptCount(8);
                protocol.setConnectionTimeout(2000);
                protocol.setConnectionUploadTimeout(2000);
                protocol.setDisableUploadTimeout(false);
                protocol.setMaxSwallowSize(0);
            });
            factory.addContextCustomizers(context -> {
                if (context.getParent() instanceof StandardHost host) {
                    host.setErrorReportValveClass(SafeErrorReportValve.class.getName());
                }
            });
        };
    }

    @Bean
    ApplicationRunner demoStarted(DemoSettings settings, org.springframework.context.ApplicationContext context) {
        return args -> LoggerFactory.getLogger(DemoConfiguration.class).info(
                "DEMO_STARTED profile={} scenarioVersion={} port={}", settings.profile(), ScenarioCatalog.VERSION,
                ((org.springframework.boot.web.server.context.WebServerApplicationContext) context).getWebServer().getPort());
    }
}
