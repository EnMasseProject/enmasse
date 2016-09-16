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
import enmasse.storage.controller.model.Flavor;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class FlavorParserTest {
    @Test
    public void testParser() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File testFile = new File("src/test/resources/flavors.json");
        Map<String, Flavor> parsed = FlavorParser.parse(mapper.readTree(testFile));

        assertThat(parsed.size(), is(1));
        assertNotNull(parsed.get("test-queue"));
        Flavor flavor = parsed.get("test-queue");
        assertThat(flavor.templateName(), is("queue-persisted"));
        assertThat(flavor.templateParameters().size(), is(1));
        assertThat(flavor.templateParameters().get("STORAGE_CAPACITY"), is("2Gi"));
    }
}
