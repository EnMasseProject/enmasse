/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.address.controller.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DestinationParserTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testEmpty() throws IOException {
        String json = "{}";
        Set<DestinationGroup> config = parsePayload(json);
        assertThat(config.size(), is(0));
    }

    @Test
    public void testParse() throws IOException {
        String json = "{\"queue1\":{\"store_and_forward\":true,\"multicast\":false,\"flavor\":\"vanilla\"}}";
        Set<DestinationGroup> config = parsePayload(json);
        assertThat(config.size(), is(1));
        DestinationGroup destGroup = config.iterator().next();
        assertThat(destGroup.getGroupId(), is("queue1"));
        Destination dest = destGroup.getDestinations().iterator().next();
        assertThat(dest.address(), is("queue1"));
        assertTrue(dest.storeAndForward());
        assertFalse(dest.multicast());
        assertThat(dest.flavor().get(), is("vanilla"));
    }

    private Set<DestinationGroup> parsePayload(String json) throws IOException {
        return DestinationParser.parse(mapper.readTree(json));
    }
}
