package com.streamflow.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps {@code DATABASE_URL} (e.g. from Render: {@code postgres://user:pass@host:5432/dbname})
 * to {@code spring.datasource.url}, {@code spring.datasource.username}, {@code spring.datasource.password}
 * so the app works when only {@code DATABASE_URL} is set (no need for {@code SPRING_DATASOURCE_*}).
 */
@SuppressWarnings("deprecation")
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DATABASE_URL = "DATABASE_URL";
    private static final String PROPERTY_SOURCE_NAME = "databaseUrlProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty(DATABASE_URL);
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        String url = databaseUrl.trim();

        if (url.startsWith("jdbc:")) {
            props.put("spring.datasource.url", url);
            // Username/password may be in URL or set via SPRING_DATASOURCE_*; no override here
        } else if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            try {
                // postgres://user:password@host:port/dbname?options
                URI uri = URI.create(url.replace("postgresql://", "postgres://"));
                String scheme = "jdbc:postgresql";
                String host = uri.getHost() != null ? uri.getHost() : "localhost";
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String db = (path != null && path.length() > 1) ? path.substring(1) : "postgres";
                String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";

                String jdbcUrl = scheme + "://" + host + ":" + port + "/" + db + query;
                props.put("spring.datasource.url", jdbcUrl);

                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    int colon = userInfo.indexOf(':');
                    if (colon > 0) {
                        props.put("spring.datasource.username", userInfo.substring(0, colon));
                        props.put("spring.datasource.password", userInfo.substring(colon + 1));
                    } else {
                        props.put("spring.datasource.username", userInfo);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Invalid DATABASE_URL: " + url, e);
            }
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
        }
    }
}
