package io.enmasse.common.model;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomResourcesTest {
    @Test
    public void testPural() {
        CustomResourceDefinition a = CustomResources.createCustomResource("enmasse.io", "v1alpha1", "Address");
        CustomResourceDefinition b = CustomResources.createCustomResource("enmasse.io", "v1alpha1", "Addressy");
        CustomResourceDefinition c = CustomResources.createCustomResource("enmasse.io", "v1alpha1", "Addr");

        assertEquals("addresses", a.getSpec().getNames().getPlural());
        assertEquals("addressies", b.getSpec().getNames().getPlural());
        assertEquals("addrs", c.getSpec().getNames().getPlural());
    }
}
