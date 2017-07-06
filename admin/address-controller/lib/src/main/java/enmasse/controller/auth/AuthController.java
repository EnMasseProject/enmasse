package enmasse.controller.auth;

import enmasse.controller.common.Watch;
import enmasse.controller.common.Watcher;
import enmasse.controller.k8s.api.AddressSpaceApi;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

import java.util.Set;

/**
 * Manages certificates issuing, revoking etc. for EnMasse services
 */
public class AuthController extends AbstractVerticle implements Watcher<AddressSpace> {

    private final CertManager certManager;
    private final AddressSpaceApi addressSpaceApi;
    private Watch watch;

    public AuthController(CertManager certManager, AddressSpaceApi addressSpaceApi) {
        this.certManager = certManager;
        this.addressSpaceApi = addressSpaceApi;
    }

    @Override
    public void start(Future<Void> startFuture) {
        vertx.executeBlocking(promise -> {
            try {
                watch = addressSpaceApi.watchAddressSpaces(this);
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }

        }, result -> {
            if (result.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        vertx.executeBlocking(promise -> {
            try {
                if (watch != null) {
                    watch.close();
                }
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                stopFuture.complete();
            } else {
                stopFuture.fail(result.cause());
            }
        });
    }

    @Override
    public void resourcesUpdated(Set<AddressSpace> addressSpaces) throws Exception {
        for (AddressSpace addressSpace : addressSpaces) {
            for (Endpoint endpoint : addressSpace.getEndpoints()) {
                if (endpoint.getCertProvider().isPresent()) {
                    certManager.issueCert(endpoint.getCertProvider().get().getSecretName(), addressSpace.getNamespace());
                }
            }
        }
    }
}
