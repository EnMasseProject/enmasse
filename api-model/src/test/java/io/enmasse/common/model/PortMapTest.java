/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.common.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.enmasse.address.model.PortMap;

public class PortMapTest {

    public static class MockData {
        private String foo;

        @JsonDeserialize(using=PortMap.Deserializer.class)
        @JsonSerialize(using=PortMap.Serializer.class)
        private Map<String, Integer> portMap1;

        @JsonDeserialize(using=PortMap.Deserializer.class)
        @JsonSerialize(using=PortMap.Serializer.class)
        private Map<String, Integer> portMap2;
        private String bar;

        public String getFoo() {
            return this.foo;
        }

        public void setFoo(final String foo) {
            this.foo = foo;
        }

        public Map<String, Integer> getPortMap1() {
            return this.portMap1;
        }

        public void setPortMap1(final Map<String, Integer> portMap1) {
            this.portMap1 = portMap1;
        }

        public Map<String, Integer> getPortMap2() {
            return this.portMap2;
        }

        public void setPortMap2(final Map<String, Integer> portMap2) {
            this.portMap2 = portMap2;
        }

        public String getBar() {
            return this.bar;
        }

        public void setBar(final String bar) {
            this.bar = bar;
        }

    }

    private static <T> T parse(String data, Class<T> clazz) throws Exception {
        return new ObjectMapper().readValue(data.replace('\'', '"'), clazz);
    }

    @Test
    public void testNull() throws Exception {
        MockData data = parse("{}", MockData.class);

        assertNotNull(data);
        assertNull(data.portMap1);
        assertNull(data.portMap2);
    }

    @Test
    public void testEmpty() throws Exception {
        MockData data = parse("{'portMap1':[]}", MockData.class);

        assertNotNull(data);
        assertNotNull(data.portMap1);
        assertEquals(data.portMap1.size(), 0);
        assertNull(data.portMap2);
    }

    @Test
    public void testValue1() throws Exception {
        MockData data = parse("{'portMap1':[{'name':'port', 'port':1}]}", MockData.class);

        assertThat(data.portMap1, hasEntry("port", 1));
        assertEquals(data.portMap1.size(), 1);
        assertNull(data.portMap2);
    }
}
