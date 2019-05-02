/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mifmif.common.regex.Generex;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public final class Templates {
    private Templates() {}

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^{}]*?)\\}");

    public static String replace(final String string, final Map<String, String> parameters) {

        if (string == null) {
            return null;
        }

        final Matcher m = VARIABLE_PATTERN.matcher(string);

        boolean result = m.find();
        if (result) {
            final StringBuffer sb = new StringBuffer();
            do {
                final String replacement = parameters.get(m.group(1));
                if (replacement == null) {
                    m.appendReplacement(sb, parameters.containsKey(m.group(1)) ? "" : escape(m.group()));
                } else {
                    m.appendReplacement(sb, escape(replace(replacement, parameters)));
                }

                result = m.find();
            } while (result);
            m.appendTail(sb);

            return sb.toString();

        } else {
            return string;
        }
    }

    private static String escape(String text) {
        if ( text == null ) {
            return null;
        }
        return text.replace("\\", "\\\\").replace("$", "\\$");
    }

    /**
     * Process a template.
     *
     * @param templateFile The file to process.
     * @param parameters The parameters to apply, may be {@code null}.
     * @return The list of processed resources.
     */
    public static KubernetesList process(final File templateFile, final Map<String, String> parameters) {

        try (InputStream input = new BufferedInputStream(new FileInputStream(templateFile))) {
            return processLocally(input, parameters);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process template", e);
        }

        // FIXME: use processLocally when it is fixed
        // return client.templates().load(templateFile).processLocally(parameters);
    }

    public static KubernetesList process(final InputStream template, final Map<String, String> parameters) {
        try {
            return processLocally(template, parameters);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process template", e);
        }
    }

    private static KubernetesList processLocally(final InputStream input, final Map<String, String> parameters) throws Exception {

        Objects.requireNonNull(parameters);

        final JsonNode tree = MAPPER.readTree(input);

        JsonNode objects = tree.get("objects");
        if (objects == null || !objects.isArray()) {
            return new KubernetesList();
        }

        final Map<String, String> finalParameters = makeParameters(parameters, tree.get("parameters"));

        KubernetesListBuilder result = new KubernetesListBuilder();

        for (JsonNode node : objects) {
            TreeTraversingParser parser = new TreeTraversingParser((JsonNode) node, MAPPER) {
                @Override
                public String getText() {
                    return replace(super.getText(), finalParameters);
                }
            };
            HasMetadata item = MAPPER.readValue(parser, HasMetadata.class);
            result.addToItems(item);
        }

        return result.build();
    }

    private static String fieldAsText(final JsonNode node, final String fieldName) {
        final JsonNode field = node.get(fieldName);
        if (field == null) {
            return null;
        }
        return field.asText();
    }

    private static Map<String, String> makeParameters(final Map<String, String> parameters, final JsonNode parametersNode) {

        if (parametersNode == null || !parametersNode.isArray()) {
            return Collections.emptyMap();
        }

        final Map<String, String> result = new HashMap<>();

        for (final JsonNode node : parametersNode) {
            if (!node.isObject()) {
                continue;
            }

            final String name = fieldAsText(node, "name");
            if (name == null || name.isEmpty()) {
                continue;
            }

            final String value;

            if (parameters.containsKey(name)) {
                value = parameters.get(name);
            } else if ("expression".equals(fieldAsText(node, "generate"))) {
                final String from = node.get("from").asText();
                final Generex generex = new Generex(from);
                value = generex.random();
            } else {
                value = fieldAsText(node, "value");
            }

            if (value == null) {
                final JsonNode requireNode = node.get("required");
                if (requireNode != null && requireNode.asBoolean(true)) {
                    throw new IllegalArgumentException(String.format("Required parameter '%s' is missing and has no default value", name));
                } else {
                    result.put(name, "");
                }
            } else {
                result.put(name, value);
            }

        }


        return result;
    }

}
