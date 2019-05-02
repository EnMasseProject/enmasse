/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.util;

import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public class TemplateTest {

    private ObjectMapper mapper;

    @BeforeEach
    public void setup() {
        this.mapper = new ObjectMapper(new YAMLFactory());
    }

    @Test
    public void testProcessLocally() throws Exception {

        final Map<String, String> parameters = new HashMap<>();
        parameters.put("INFRA_UUID", "81932f70-20a9-11e9-b293-c85b762e5a2c");
        parameters.put("INFRA_NAMESPACE", "enmasse-infra");
        parameters.put("ADDRESS_SPACE_PLAN", "plan1");
        parameters.put("ADDRESS_SPACE", "myspace1");
        parameters.put("ADDRESS_SPACE_NAMESPACE", "ns");
        parameters.put("CONSOLE_SECRET", "secret1");
        parameters.put("MESSAGING_SECRET", "secret2");
        parameters.put("AUTHENTICATION_SERVICE_HOST", "host1");
        parameters.put("AUTHENTICATION_SERVICE_PORT", "123");
        parameters.put("AUTHENTICATION_SERVICE_CA_CERT", "abc");
        parameters.put("STANDARD_INFRA_CONFIG_NAME", "configName1");

        try (
                InputStream template = TemplateTest.class.getResourceAsStream("template_1.yaml");
                InputStream result = TemplateTest.class.getResourceAsStream("template_1.result.yaml");) {

            final KubernetesList list = normalize(Templates.process(template, parameters));
            final KubernetesList expected = normalize(mapper.readValue(result, KubernetesList.class));

            assertThat(list, CoreMatchers.is(expected));
            // the next statements helps finding differences in the case of errors:
            // assertEquals(mapper.writeValueAsString(expected), mapper.writeValueAsString(list));
        }

    }

    @Test
    public void testMissingParameter() throws Exception {

        final Map<String, String> parameters = new HashMap<>();

        try (InputStream template = TemplateTest.class.getResourceAsStream("template_1.yaml")) {
            assertThrows(RuntimeException.class, () -> {
                Templates.process(template, parameters);
            });
        }

    }


    private KubernetesList normalize(KubernetesList list) throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder(list)
                .withNewMetadata()
                .endMetadata();

        return mapper.readValue(mapper.writeValueAsBytes(builder.build()), KubernetesList.class);
    }


}
