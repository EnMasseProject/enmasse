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

package enmasse.storage.controller.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.storage.controller.model.AddressConfig;
import enmasse.storage.controller.model.Destination;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class AddressConfigParserTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testEmpty() throws IOException {
        String json = "{}";
        AddressConfig config = parsePayload(json);
        assertThat(config.destinations().size(), is(0));
    }

    @Test
    public void testParse() throws IOException {
        String json = "{\"queue1\":{\"store_and_forward\":true,\"multicast\":false,\"flavor\":\"vanilla\"}}";
        AddressConfig config = parsePayload(json);
        assertThat(config.destinations().size(), is(1));
        Destination dest = config.destinations().iterator().next();
        assertThat(dest.address(), is("queue1"));
        assertTrue(dest.storeAndForward());
        assertFalse(dest.multicast());
        assertThat(dest.flavor(), is("vanilla"));
    }

    private AddressConfig parsePayload(String json) throws IOException {
        return AddressConfigParser.parse(mapper.readTree(json));
    }
}
