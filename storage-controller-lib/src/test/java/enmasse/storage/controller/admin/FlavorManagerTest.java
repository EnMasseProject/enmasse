package enmasse.storage.controller.admin;

import enmasse.storage.controller.model.Flavor;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FlavorManagerTest {
    private FlavorManager flavorManager;

    @Before
    public void setup() {
        flavorManager = new FlavorManager();
    }

    @Test
    public void testManager() {
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put("flavor1", new Flavor.Builder().templateName("template1").build());
        flavorMap.put("flavor2", new Flavor.Builder().templateName("template2").build());
        flavorMap.put("flavor3", new Flavor.Builder().templateName("template1").templateParameter("BROKER_IMAGE", "myimage").build());

        flavorManager.flavorsUpdated(flavorMap);

        assertThat(flavorManager.getFlavor("flavor1", 0).templateName(), is("template1"));
        assertThat(flavorManager.getFlavor("flavor1", 0).templateParameters().size(), is(0));
        assertThat(flavorManager.getFlavor("flavor2", 0).templateName(), is("template2"));
        assertThat(flavorManager.getFlavor("flavor2", 0).templateParameters().size(), is(0));
        assertThat(flavorManager.getFlavor("flavor3", 0).templateName(), is("template1"));
        assertThat(flavorManager.getFlavor("flavor3", 0).templateParameters().size(), is(1));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testTimeout() {
        flavorManager.getFlavor("flavor1", 1000);
    }
}
