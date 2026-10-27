package com.ott.common.neon;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Maps the Neon-provided {@code DATABASE_URL} (postgresql://user:pass@host/db?...) onto
 * {@code spring.datasource.*}, since Spring/JDBC cannot consume that URL form directly.
 *
 * <p>Opt-in per service with {@code neon.datasource.enabled=true}; does nothing when
 * {@code DATABASE_URL} is absent, so local docker-compose Postgres keeps working.
 * {@code DATABASE_URL} is the pooled endpoint, which is what a long-lived app pool should use;
 * schema migrations must use {@code DATABASE_URL_UNPOOLED}.
 */
public class NeonDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "neonDatasource";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("neon.datasource.enabled", Boolean.class, false)) {
            return;
        }
        String url = environment.getProperty("DATABASE_URL");
        if (url == null || url.isBlank()) {
            return;
        }
        URI uri = URI.create(url.trim());
        String userInfo = uri.getRawUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalStateException("DATABASE_URL must contain user:password credentials");
        }
        int sep = userInfo.indexOf(':');
        String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
        String query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath() + query);
        props.put("spring.datasource.username", decode(userInfo.substring(0, sep)));
        props.put("spring.datasource.password", decode(userInfo.substring(sep + 1)));
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, props));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
