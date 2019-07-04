package io.enmasse.iot.registry.infinispan;

import org.eclipse.hono.deviceregistry.FileBasedDeviceBackend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A device backend that keeps all data in memory but is backed by a file. This is done by leveraging and unifying
 * {@link CacheRegistrationService} and {@link CacheCredentialService}
 */
public class DeviceBackend extends FileBasedDeviceBackend {

    private final CacheRegistrationService registrationService;
    private final CacheCredentialService credentialsService;

    /**
     * Create a new instance.
     *
     * @param registrationService an implementation of registration service.
     * @param credentialsService an implementation of credentials service.
     */
    @Autowired
    public DeviceBackend(
            @Qualifier("serviceImpl") final CacheRegistrationService registrationService,
            @Qualifier("serviceImpl") final CacheCredentialService credentialsService) {
        this.registrationService = registrationService;
        this.credentialsService = credentialsService;
    }

}
