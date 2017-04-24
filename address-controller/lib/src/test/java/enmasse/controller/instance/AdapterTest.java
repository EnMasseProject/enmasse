package enmasse.controller.instance;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;

/**
 * TODO: Description
 */
public class AdapterTest {
    @Test
    public void testClient() {
        Config config = new Config();
        //config.setMasterUrl("https://192.168.42.254:8443");
        config.setMasterUrl("https://192.168.99.100:8443");
        config.setUsername("developer");
        config.setOauthToken("ztzEk3iuGRp7H972hExWijlQEGwSYByQJUwROA3ajxk");
        config.setNamespace("myproject");
        OpenShiftClient client = new DefaultOpenShiftClient(config);
        ConfigMapList list = client.configMaps().list();
        System.out.println("Config maps: " + list.getItems().size());
        for (ConfigMap map : list.getItems()) {
            System.out.println("Map: " + map.getMetadata().getName());
        }

        if (client.isAdaptable(OpenShiftClient.class)) {
            System.out.println("Adaptable to openshift client!");
        } else {
            System.out.println("Not adaptable to openshift client");
        }
    }
}
