package quilt.config.generator;

import com.openshift.restclient.model.IResource;
import org.junit.Test;
import quilt.config.model.Broker;
import quilt.config.model.Config;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author lulf
 */
public class ConfigGeneratorTest {
    @Test
    public void testSkipNoStore() {
        ConfigGenerator generator = new ConfigGenerator(null);
        List<IResource> resources = generator.generate(new Config(Arrays.asList(new Broker("foo", true, false), new Broker("bar", false, false))));
        assertThat(resources.size(), is(1));
    }
}
