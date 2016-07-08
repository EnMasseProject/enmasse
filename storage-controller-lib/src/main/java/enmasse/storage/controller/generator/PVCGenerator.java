package enmasse.storage.controller.generator;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.internal.restclient.model.volume.PersistentVolumeClaim;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;
import enmasse.storage.controller.admin.FlavorManager;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.FlavorConfig;

import java.util.Collections;

/**
 * @author Ulf Lilleengen
 */
public class PVCGenerator {
    private final ResourceFactory factory;

    public PVCGenerator(IClient osClient) {
        this.factory = new ResourceFactory(osClient);
    }

    public IPersistentVolumeClaim generate(Destination dest) {
        FlavorConfig flavorConfig = dest.flavor();
        PersistentVolumeClaim claim = factory.create("v1", ResourceKind.PVC);
        claim.setName("pvc-" + dest.address());
        claim.setAccessModes(Collections.singleton("ReadWriteOnce"));
        claim.setRequestedStorage(flavorConfig.storageConfig().size());
        return claim;
    }
}
