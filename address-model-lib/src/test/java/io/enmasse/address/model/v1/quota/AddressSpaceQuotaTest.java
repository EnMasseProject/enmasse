/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.quota;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressSpaceQuotaTest {
    @Test
    public void testSerialization() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1alpha1\"," +
                "\"kind\":\"AddressSpaceQuota\"," +
                "\"metadata\":{" +
                "  \"name\":\"developer-quota\"" +
                "}," +
                "\"spec\": {" +
                "  \"user\": \"developer\"," +
                "  \"rules\": [" +
                "    { \"count\": 1, \"type\": \"standard\", \"plan\": \"unlimited-standard\" }," +
                "    { \"count\": 2, \"type\": \"brokered\", \"plan\": \"unlimited-brokered\" }" +
                "  ]" +
                "}}";

        ObjectMapper mapper = new ObjectMapper();
        AddressSpaceQuota quota = mapper.readValue(json, AddressSpaceQuota.class);

        assertEquals("developer-quota", quota.getMetadata().getName());
        assertEquals("developer", quota.getSpec().getUser());

        List<AddressSpaceQuotaRule> rules = quota.getSpec().getRules();
        assertEquals(2, rules.size());
        assertRule(rules, "standard", "unlimited-standard", 1);
        assertRule(rules, "brokered", "unlimited-brokered", 2);

        String output = mapper.writeValueAsString(quota);
        ObjectNode expected = mapper.readValue(json, ObjectNode.class);
        ObjectNode actual = mapper.readValue(output, ObjectNode.class);

        assertEquals(expected, actual);
    }

    private static void assertRule(List<AddressSpaceQuotaRule> rules, String type, String plan, int count) {
        boolean found = false;
        for (AddressSpaceQuotaRule rule : rules) {
            if (rule.getType().equals(type) && rule.getPlan().equals(plan) && count == rule.getCount()) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Unable to find matching rule in list");
    }
}
