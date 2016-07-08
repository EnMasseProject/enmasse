package enmasse.storage.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.restclient.model.volume.VolumeType;
import enmasse.storage.controller.model.FlavorConfig;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Ulf Lilleengen
 */
public class FlavorManagerTest {
    private FlavorManager manager;

    @Before
    public void setUp() {
        manager = new FlavorManager();
    }

    @Test
    public void testParseFlavors() throws IOException {
        manager.configUpdated(new ObjectMapper().readTree(new File("src/test/resources/flavors.json")));

        FlavorConfig config = manager.getFlavor("vanilla");
        assertTrue(config.isShared());
        assertThat(config.storageConfig().volumeType(), is(VolumeType.EMPTY_DIR));
        assertThat(config.storageConfig().mountPath(), is("/var/run/artemis"));

        config = manager.getFlavor("chili");
        assertFalse(config.isShared());
        assertThat(config.storageConfig().volumeType(), is(VolumeType.PERSISTENT_VOLUME_CLAIM));
        assertThat(config.storageConfig().mountPath(), is("/var/run/artemis"));
        assertThat(config.routerSecretName(), is("qdrouterd-certs"));

        config = manager.getFlavor("caramel");
        assertTrue(config.isShared());
        assertThat(config.storageConfig().volumeType(), is(VolumeType.PERSISTENT_VOLUME_CLAIM));
        assertThat(config.storageConfig().mountPath(), is("/var/lib/activemq"));
    }
}
