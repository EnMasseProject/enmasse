/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.controller.auth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.k8s.api.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages certificates issuing, revoking etc. for EnMasse services
 */
public class AuthController extends AbstractVerticle implements Watcher<AddressSpace> {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class.getName());

    private final CertManager certManager;
    private final AddressSpaceApi addressSpaceApi;
    private final EventLogger eventLogger;
    private Watch watch;

    public AuthController(CertManager certManager,
                          AddressSpaceApi addressSpaceApi,
                          EventLogger eventLogger) {
        this.certManager = certManager;
        this.addressSpaceApi = addressSpaceApi;
        this.eventLogger = eventLogger;
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
                log.info("Started auth controller");
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
            issueAddressSpaceCert(addressSpace);
            issueComponentCertificates(addressSpace);
            issueExternalCertificates(addressSpace);
        }
    }

    private void issueExternalCertificates(AddressSpace addressSpace) throws Exception {
        for (Endpoint endpoint : addressSpace.getEndpoints()) {
            if (endpoint.getCertProvider().isPresent()) {
                String secretName = endpoint.getCertProvider().get().getSecretName();
                certManager.issueRouteCert(secretName, addressSpace.getNamespace());
            }
        }
    }


    private void issueAddressSpaceCert(final AddressSpace addressSpace)
    {
        vertx.executeBlocking(promise -> {
            try {
                final String addressSpaceCaSecretName = KubeUtil.getAddressSpaceCaSecretName(addressSpace.getNamespace());
                if(!certManager.certExists(addressSpaceCaSecretName)) {
                    certManager.createSelfSignedCertSecret(addressSpaceCaSecretName);
                    //put crt into address space

                }
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                log.info("Issued addressspace ca certificates: {}", result.result());
                eventLogger.log("CertCreated", "Created address space CA for " + addressSpace.getName(), "Normal");
            } else {
                log.warn("Error issuing addressspace ca certificate", result.cause());
                eventLogger.log("CertCreateFailed", "Failed creating certificate for address space " + addressSpace.getName(), "Warning");
            }
        });
    }

    private void issueComponentCertificates(AddressSpace addressSpace) {
        vertx.executeBlocking((Future<List<Cert>> promise) -> {
            try {
                final String caSecretName = KubeUtil.getAddressSpaceCaSecretName(addressSpace.getNamespace());
                promise.complete(certManager.listComponents(addressSpace.getNamespace()).stream()
                        .filter(component -> !certManager.certExists(component))
                        .map(certManager::createCsr)
                        .map(request -> certManager.signCsr(request,
                                                            caSecretName))
                        .map(cert -> {
                            certManager.createSecret(cert, caSecretName);
                            return cert; })
                        .collect(Collectors.toList()));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                List<Cert> components = result.result();
                if (!components.isEmpty()) {
                    log.info("Issued component certificates: {}", result.result());
                    eventLogger.log("CertCreated", "Created component certificates for " + addressSpace.getName(), "Normal");
                }
            } else {
                log.warn("Error issuing component certificates", result.cause());
                eventLogger.log("CertCreateFailed", "Error creating component certificates for " + addressSpace.getName(), "Warning");
            }
        });
    }
}
