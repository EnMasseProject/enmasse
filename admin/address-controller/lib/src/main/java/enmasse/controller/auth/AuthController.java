package enmasse.controller.auth;

import enmasse.controller.common.Watch;
import enmasse.controller.common.Watcher;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Instance;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

import java.util.Set;

/**
 * Manages certificates issuing, revoking etc. for EnMasse services
 */
public class AuthController extends AbstractVerticle implements Watcher<Instance> {

    private final CertManager certManager;
    private final InstanceApi instanceApi;
    private Watch watch;

    public AuthController(CertManager certManager, InstanceApi instanceApi) {
        this.certManager = certManager;
        this.instanceApi = instanceApi;
    }

    @Override
    public void start(Future<Void> startFuture) {
        vertx.executeBlocking(promise -> {
            try {
                watch = instanceApi.watchInstances(this);
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
    public void resourcesUpdated(Set<Instance> instances) throws Exception {
        for (Instance instance : instances) {
            certManager.issueCert(instance.certSecret(), instance.id().getNamespace());
        }
    }
}
