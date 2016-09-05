package enmasse.storage.controller.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.storage.controller.model.AddressConfig;
import enmasse.storage.controller.model.Destination;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author Ulf Lilleengen
 */
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
