package com.streamflow.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads a {@code .env} file from the working directory (or project root when run via Maven)
 * and adds its entries to the environment so that {@code application.properties}
 * placeholders like {@code ${SPRING_DATASOURCE_URL:...}} resolve correctly.
 * <p>
 * Spring Boot does not load {@code .env} by default; this processor makes local
 * development work with {@code mvn spring-boot:run} when a {@code .env} file is present.
 */
@SuppressWarnings("deprecation")
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotEnvProperties";
    private static final Pattern ENV_LINE = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)=(.*)$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envPath = findEnvFile();
        if (envPath == null || !Files.isRegularFile(envPath)) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try {
            for (String line : Files.readAllLines(envPath)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                Matcher m = ENV_LINE.matcher(line);
                if (m.matches()) {
                    String key = m.group(1).trim();
                    String value = unquote(m.group(2).trim());
                    if (!value.isEmpty() || environment.getProperty(key) == null) {
                        props.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            // Ignore: .env may not exist or be unreadable
            return;
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
        }
    }

    private static Path findEnvFile() {
        // Prefer current working directory (e.g. streamflow-be when running from there)
        Path cwd = Paths.get(System.getProperty("user.dir", ".")).normalize();
        Path inCwd = cwd.resolve(".env");
        if (Files.isRegularFile(inCwd)) {
            return inCwd;
        }
        // When run from repo root, .env might be in streamflow-be
        Path inModule = cwd.resolve("streamflow-be").resolve(".env").normalize();
        if (Files.isRegularFile(inModule)) {
            return inModule;
        }
        return null;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
