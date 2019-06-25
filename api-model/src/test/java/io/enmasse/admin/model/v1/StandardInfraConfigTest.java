/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;


public class StandardInfraConfigTest {

    @Test
    public void testParseEmptySpec() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final URL url = StandardInfraConfigTest.class.getResource("resources/standard-infra-empty.json");
        final StandardInfraConfig value = mapper.readValue(url, StandardInfraConfig.class);

        assertThat(value, notNullValue());

        assertThat(value.getMetadata(), notNullValue());
        assertThat(value.getMetadata().getName(), is("standard"));

        assertThat(value.getSpec(), notNullValue());


    }

}
