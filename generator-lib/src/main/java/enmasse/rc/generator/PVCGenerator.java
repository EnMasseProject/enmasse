package enmasse.rc.generator;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.internal.restclient.model.volume.PersistentVolumeClaim;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;
import enmasse.rc.model.Destination;

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
        PersistentVolumeClaim claim = factory.create("v1", ResourceKind.PVC);
        claim.setName("pvc-" + dest.address());
        claim.setAccessModes(Collections.singleton("ReadWriteOnce"));
        claim.setRequestedStorage("1Gi"); // TODO: Determine this based on some external property
        return claim;
    }
}
