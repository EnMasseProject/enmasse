/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class StatementConfiguration {

    public static final Path DEFAULT_PATH = Paths.get("/etc/config/sql");

    private static final Logger log = LoggerFactory.getLogger(StatementConfiguration.class);

    private final Map<String, Statement> statements;
    private final Object[] formatArguments;

    private StatementConfiguration(final Map<String, Statement> statements, final Object[] formatArguments) {
        this.statements = statements;
        this.formatArguments = formatArguments;
    }

    public StatementConfiguration overrideWith(final Path path, boolean ignoreMissing) throws IOException {

        if (ignoreMissing && !Files.exists(path)) {
            log.info("Ignoring missing statement configuration file: {}", path);
            return this;
        }

        log.info("Loading statement configuration file: {}", path);

        try (InputStream input = Files.newInputStream(path)) {
            return overrideWith(input, false);
        }

    }

    public StatementConfiguration overrideWith(final InputStream input, boolean ignoreMissing) {
        if (input == null) {
            if (ignoreMissing) {
                return this;
            } else {
                throw new IllegalArgumentException("Missing input");
            }
        }

        final Yaml yaml = createYamlParser();
        /*
         * we must load using "load(input)" not "loadAs(input, Map.class)", because the
         * latter would require to construct a class (Map) by name, which we do not support.
         */
        final Map<String, Object> properties = yaml.load(input);
        if (properties == null) {
            // we could read the source, but it was empty
            return this;
        }
        return overrideWith(properties);
    }

    private StatementConfiguration overrideWith(final Map<String, Object> properties) {

        final Map<String, Statement> result = new HashMap<>(this.statements);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            final String key = entry.getKey();

            // if key is set, but not a string ...
            if (entry.getValue() != null && (!(entry.getValue() instanceof String))) {
                // ... fail
                throw new IllegalArgumentException(String.format("Key '%s' is not of type string: %s", entry.getKey(), entry.getValue().getClass()));
            }

            final String value = entry.getValue() != null ? entry.getValue().toString() : null;
            if (value == null || value.isBlank()) {
                // remove key
                result.remove(key);
                continue;
            }

            final Statement s = Statement.statement(value, this.formatArguments);
            result.put(key, s);
        }

        return new StatementConfiguration(result, this.formatArguments);
    }

    public Optional<Statement> getStatement(final String key) {
        return Optional.ofNullable(this.statements.get(key));
    }

    public Statement getRequiredStatment(final String key) {
        return getStatement(key)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Statement with key '%s' not found", key)));
    }

    public static StatementConfiguration empty(final Object... formatArguments) {
        return new StatementConfiguration(Collections.emptyMap(), formatArguments);
    }

    public StatementConfiguration overideWithDefaultPattern(final String basename, final String dialect, final Class<?> clazz, final Path path) throws IOException {

        final String base = basename + ".sql.yaml";
        final String dialected = basename + "." + dialect + ".sql.yaml";

        try (
                InputStream resource = clazz.getResourceAsStream(base);
                InputStream dialectResource = clazz.getResourceAsStream(dialected);) {

            final Path overridePath = path.resolve(basename + ".sql.yaml");

            log.debug("Loading - class: {}, name: {}, input: {}", clazz, base, resource);
            log.debug("Loading - class: {}, name: {}, input: {}", clazz, dialected, dialectResource);
            log.debug("Loading - path: {}", overridePath);

            return this
                    .overrideWith(resource, false)
                    .overrideWith(dialectResource, true)
                    .overrideWith(overridePath, true);
        }

    }

    public void dump(final Logger logger) {

        if (!logger.isInfoEnabled()) {
            return;
        }

        logger.info("Dumping statement configuration");
        logger.info("Format arguments: {}", this.formatArguments);

        final String[] keys = this.statements.keySet().toArray(String[]::new);
        Arrays.sort(keys);
        logger.info("Statements:");
        for (String key : keys) {
            logger.info("{}\n{}", key, this.statements.get(key));
        }

    }

    /**
     * Create a YAML parser which reject creating arbitrary Java objects.
     * @return A new YAML parser, never returns {@code null}.
     */
    static Yaml createYamlParser() {
        return new Yaml(new Constructor() {
            @Override
            protected Class<?> getClassForName(String name) throws ClassNotFoundException {
                throw new IllegalArgumentException("Class instantiation is not supported");
            }
        });
    }

}
