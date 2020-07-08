/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import com.google.common.collect.Lists;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingEndpointPort;
import io.enmasse.api.model.MessagingInfrastructureBuilder;
import io.enmasse.api.model.MessagingProjectBuilder;
import io.enmasse.iot.model.v1.AdapterConfig;
import io.enmasse.iot.model.v1.AdapterConfigFluent;
import io.enmasse.iot.model.v1.AdaptersConfig;
import io.enmasse.iot.model.v1.AdaptersConfigFluent;
import io.enmasse.iot.model.v1.AdaptersConfigFluent.AmqpNested;
import io.enmasse.iot.model.v1.AdaptersConfigFluent.HttpNested;
import io.enmasse.iot.model.v1.AdaptersConfigFluent.LoraWanNested;
import io.enmasse.iot.model.v1.AdaptersConfigFluent.MqttNested;
import io.enmasse.iot.model.v1.AdaptersConfigFluent.SigfoxNested;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTConfigFluent.SpecNested;
import io.enmasse.iot.model.v1.IoTConfigSpec;
import io.enmasse.iot.model.v1.IoTConfigSpecFluent.AdaptersNested;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.framework.TestPlanInfo;
import io.enmasse.systemtest.iot.IoTTestSession.Builder.PreDeployProcessor;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.ThrowingCallable;
import io.enmasse.systemtest.utils.ThrowingConsumer;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.enmasse.systemtest.amqp.AmqpConnectOptions.defaultQueue;
import static io.enmasse.systemtest.condition.OpenShiftVersion.OCP4;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.AMQP;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.HTTP;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.MQTT;
import static io.enmasse.systemtest.platform.Kubernetes.isOpenShiftCompatible;
import static io.enmasse.systemtest.utils.Conditions.condition;
import static io.enmasse.systemtest.utils.TestUtils.waitUntilConditionOrFail;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;

public final class IoTTestSession implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(IoTTestSession.class);
    private static final String IOT_PROJECT_NAMESPACE = "iot-systemtests";

    public enum Adapter {
        AMQP(AdaptersConfig::getAmqp) {
            @Override
            public IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer) {
                return Adapter.editAdapter(config, AdaptersConfigFluent::editOrNewAmqp, AmqpNested::endAmqp, a -> {
                    consumer.accept(a);
                    return a;
                });
            }
        },
        HTTP(AdaptersConfig::getHttp) {
            @Override
            public IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer) {
                return Adapter.editAdapter(config, AdaptersConfigFluent::editOrNewHttp, HttpNested::endHttp, a -> {
                    consumer.accept(a);
                    return a;
                });
            }
        },
        MQTT(AdaptersConfig::getMqtt) {
            @Override
            public IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer) {
                return Adapter.editAdapter(config, AdaptersConfigFluent::editOrNewMqtt, MqttNested::endMqtt, a -> {
                    consumer.accept(a);
                    return a;
                });
            }
        },
        SIGFOX(AdaptersConfig::getSigfox) {
            @Override
            public IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer) {
                return Adapter.editAdapter(config, AdaptersConfigFluent::editOrNewSigfox, SigfoxNested::endSigfox, a -> {
                    consumer.accept(a);
                    return a;
                });
            }
        },
        LORAWAN(AdaptersConfig::getLoraWan) {
            @Override
            public IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer) {
                return Adapter.editAdapter(config, AdaptersConfigFluent::editOrNewLoraWan, LoraWanNested::endLoraWan, a -> {
                    consumer.accept(a);
                    return a;
                });
            }
        },
        ;

        private static <X extends AdapterConfigFluent<X>> IoTConfigBuilder editAdapter(
                final IoTConfigBuilder config,
                final Function<AdaptersNested<SpecNested<IoTConfigBuilder>>, X> editOrNew,
                final Function<X, AdaptersNested<SpecNested<IoTConfigBuilder>>> end,
                final Function<X, X> editor) {

            final AdaptersNested<SpecNested<IoTConfigBuilder>> a = config
                    .editOrNewSpec()
                    .editOrNewAdapters();

            return editOrNew
                    .andThen(editor)
                    .andThen(end)
                    .apply(a)

                    .endAdapters()
                    .endSpec();

        }

        Adapter(final Function<AdaptersConfig, ? extends AdapterConfig> getter) {
            this.getter = getter;
        }

        private final Function<AdaptersConfig, ? extends AdapterConfig> getter;

        public abstract IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer);

        public Optional<AdapterConfig> getConfig(IoTConfig config) {
            return Optional
                    .ofNullable(config)
                    .map(IoTConfig::getSpec)
                    .map(IoTConfigSpec::getAdapters)
                    .map(getter);
        }

        public <T> T apply(final IoTConfig config, final Function<Optional<AdapterConfig>, T> function) {
            return function.apply(getConfig(config));
        }

        public IoTConfigBuilder enable(final IoTConfigBuilder config, boolean enabled) {
            return edit(config, a -> a.withEnabled(enabled));
        }

        public IoTConfigBuilder enable(final IoTConfigBuilder config) {
            return enable(config, true);
        }

        public IoTConfigBuilder disable(final IoTConfigBuilder config) {
            return enable(config, false);
        }

        /**
         * Check if an adapter is enabled.
         * <p>
         * The adapter is only disabled if the field {@code enabled} is set to {@code value}.
         * In all other cases, like null values or missing fields, the adapter is considered enabled.
         *
         * @param config The configuration to check.
         * @return {@code true} if the adapter is enabled, {@code false} otherwise.
         */
        public boolean isEnabled(final IoTConfig config) {
            return getConfig(config)
                    .map(adapterConfig -> adapterConfig.getEnabled() == null || Boolean.TRUE.equals(adapterConfig.getEnabled()))
                    .orElse(Boolean.TRUE);
        }

        public String getResourceName() {
            return String.format("iot-%s-adapter", name().toLowerCase());
        }
    }

    @FunctionalInterface
    public interface Code {
        void run(IoTTestSession session) throws Exception;
    }

    public class Device {

        private final String deviceId;
        private String authId;
        private String password;
        private PrivateKey key;
        private X509Certificate certificate;

        private Device(String deviceId) {
            this.deviceId = deviceId;
        }

        public Device register() throws Exception {
            IoTTestSession.this.registryClient.registerDevice(getTenantId(), this.deviceId);
            return this;
        }

        /**
         * Set username and password to random combination.
         *
         * @return This instance, for chained method calls.
         * @throws Exception in case anything went wrong.
         */
        public Device setPassword() throws Exception {
            return setPassword(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        }

        public Device setPassword(final String authId, final String password) throws Exception {
            this.authId = authId;
            this.password = password;

            var pwd = CredentialsRegistryClient.createPlainPasswordCredentialsObject(authId, password, null);
            IoTTestSession.this.credentialsClient.setCredentials(getTenantId(), this.deviceId, singletonList(pwd));
            return this;
        }

        /**
         * Use a different password then stored in the device registry (using {@link #setPassword()}.
         * <p>
         * Uses a new random password.
         *
         * @return This instance, for chained method calls.
         */
        public Device overridePassword() {
            this.password = UUID.randomUUID().toString();
            return this;
        }

        /**
         * Use a different password then stored in the device registry (using {@link #setPassword()}.
         *
         * @param password The new password to use.
         * @return This instance, for chained method calls.
         */
        public Device overridePassword(final String password) {
            this.password = password;
            return this;
        }

        public Device enableX509(io.enmasse.systemtest.iot.DeviceCertificateManager.Device device) throws Exception {
            return enableX509(device.getKey().getPrivate(), device.getCertificate());
        }

        public Device enableX509(final PrivateKey key, final X509Certificate certificate) throws Exception {
            this.key = key;
            this.certificate = certificate;

            var x509 = CredentialsRegistryClient.createX509CertificateCredentialsObject(certificate.getSubjectX500Principal().getName(), null);
            IoTTestSession.this.credentialsClient.setCredentials(getTenantId(), this.deviceId, singletonList(x509));
            return this;
        }

        /**
         * Create a new http adapter client.
         *
         * @return The new instance. It will automatically be closed when the test session is being cleaned
         * up.
         */
        public HttpAdapterClient createHttpAdapterClient() throws Exception {
            return createHttpAdapterClient(null);
        }

        /**
         * Create a new http adapter client.
         *
         * @param tlsVersions The supported TLS versions.
         * @return The new instance. It will automatically be closed when the test session is being cleaned
         * up.
         */
        public HttpAdapterClient createHttpAdapterClient(final Set<String> tlsVersions) throws Exception {
            if (this.key != null) {
                return IoTTestSession.this.createHttpAdapterClient(this.key, this.certificate, tlsVersions);
            } else {
                return IoTTestSession.this.createHttpAdapterClient(this.authId, this.password, tlsVersions);
            }
        }

        /**
         * Create a new mqtt adapter client.
         *
         * @return The new instance. It will automatically be closed when the test session is being cleaned
         * up.
         */
        public MqttAdapterClient createMqttAdapterClient() throws Exception {
            if (key != null) {
                return IoTTestSession.this.createMqttAdapterClient(this.deviceId, this.key, this.certificate);
            } else {
                return IoTTestSession.this.createMqttAdapterClient(this.deviceId, this.authId, this.password);
            }
        }

        /**
         * Create a new AMQP adapter client.
         *
         * @return The new instance. It will automatically be closed when the test session is being cleaned
         * up.
         */
        public AmqpAdapterClient createAmqpAdapterClient() throws Exception {
            return createAmqpAdapterClient(null);
        }

        /**
         * Create a new AMQP adapter client.
         *
         * @param tlsVersions The supported TLS versions.
         * @return The new instance. It will automatically be closed when the test session is being cleaned
         * up.
         */
        public AmqpAdapterClient createAmqpAdapterClient(final Set<String> tlsVersions) throws Exception {
            if (this.key != null) {
                return IoTTestSession.this.createAmqpAdapterClient(this.key, this.certificate, tlsVersions);
            } else {
                return IoTTestSession.this.createAmqpAdapterClient(this.authId, this.password, tlsVersions);
            }
        }

        public String getTenantId() {
            return IoTTestSession.this.getTenantId();
        }
    }

    private final Vertx vertx;
    private final IoTConfig config;
    private final IoTProject project;
    private final Consumer<Throwable> exceptionHandler;
    private final List<ThrowingCallable> cleanup;
    private final DeviceRegistryClient registryClient;
    private final CredentialsRegistryClient credentialsClient;
    private final AmqpClient consumerClient;

    private IoTTestSession(
            final Vertx vertx,
            final IoTConfig config,
            final IoTProject project,
            final DeviceRegistryClient registryClient,
            final CredentialsRegistryClient credentialsClient,
            final AmqpClient consumerClient,
            final Consumer<Throwable> exceptionHandler,
            final List<ThrowingCallable> cleanup) {

        this.vertx = vertx;

        this.config = config;
        this.project = project;

        this.registryClient = registryClient;
        this.credentialsClient = credentialsClient;

        this.consumerClient = consumerClient;

        this.exceptionHandler = exceptionHandler;
        this.cleanup = cleanup;

    }

    public String getTenantId() {
        return getTenantId(this.project);
    }

    public IoTConfig getConfig() {
        return this.config;
    }

    public IoTProject getProject() {
        return this.project;
    }

    private static String getTenantId(final IoTProject project) {
        return project.getMetadata().getNamespace() + "." + project.getMetadata().getName();
    }

    private HttpAdapterClient createHttpAdapterClient(final PrivateKey key, final X509Certificate certificate, final Set<String> tlsVersions) throws Exception {

        var endpoint = Kubernetes.getInstance().getExternalEndpoint("iot-http-adapter");
        var result = new HttpAdapterClient(this.vertx, endpoint, key, certificate, tlsVersions);
        this.cleanup.add(result::close);

        return result;

    }

    private HttpAdapterClient createHttpAdapterClient(final String authId, final String password, final Set<String> tlsVersions) throws Exception {

        var endpoint = Kubernetes.getInstance().getExternalEndpoint("iot-http-adapter");
        var result = new HttpAdapterClient(this.vertx, endpoint, authId, getTenantId(), password, tlsVersions);
        this.cleanup.add(result::close);

        return result;

    }

    private AmqpAdapterClient createAmqpAdapterClient(final PrivateKey key, final X509Certificate certificate, final Set<String> tlsVersions)
            throws Exception {

        var endpoint = Kubernetes.getInstance().getExternalEndpoint("iot-amqp-adapter");
        var result = new AmqpAdapterClient(this.vertx, endpoint, key, certificate, tlsVersions);
        this.cleanup.add(result::close);
        result.connect();

        return result;

    }

    private AmqpAdapterClient createAmqpAdapterClient(final String authId, final String password, final Set<String> tlsVersions) throws Exception {

        var endpoint = Kubernetes.getInstance().getExternalEndpoint("iot-amqp-adapter");
        var result = new AmqpAdapterClient(this.vertx, endpoint, authId, getTenantId(), password, tlsVersions);
        this.cleanup.add(result::close);
        result.connect();

        return result;

    }

    private MqttAdapterClient createMqttAdapterClient(final String deviceId, final PrivateKey key, final X509Certificate certificate) throws Exception {

        var endpoint = Kubernetes.getInstance().getExternalEndpoint("iot-mqtt-adapter");
        var result = MqttAdapterClient.create(endpoint, deviceId, key, certificate);
        this.cleanup.add(result::close);

        return result;

    }

    private MqttAdapterClient createMqttAdapterClient(final String deviceId, final String authId, final String password) throws Exception {

        var endpoint = Kubernetes.getInstance().getExternalEndpoint("iot-mqtt-adapter");
        var result = MqttAdapterClient.create(endpoint, deviceId, authId, getTenantId(), password);
        this.cleanup.add(result::close);

        return result;

    }

    public AmqpClient getConsumerClient() {
        return this.consumerClient;
    }

    /**
     * Run code for the session, properly handling exceptions.
     *
     * @param code The code to run.
     * @throws Exception In case anything went wrong.
     */
    public void run(final Runnable code) throws Exception {
        run((Code) session -> code.run());
    }

    /**
     * Run code for the session, properly handling exceptions.
     *
     * @param code The code to run.
     * @throws Exception In case anything went wrong.
     */
    public void run(final Code code) throws Exception {
        /*
         * We need an inner try-catch in order to only handle the exception of the test code.
         * The exception of the deploy method is handled by the deploy method itself.
         */
        try {
            code.run(this);
        } catch (Throwable e) {
            // we need to catch Throwables as unit test assertions
            // are based on Error instead of Exception.
            if (log.isDebugEnabled()) {
                log.debug("Caught exception during test", e);
            } else {
                log.error("Caught exception during test, running exception handler");
            }
            this.exceptionHandler.accept(e);
            throw e;
        }
    }

    /**
     * Close the instance and run remaining cleanup tasks.
     */
    @Override
    public void close() throws Exception {

        var e = cleanup(this.cleanup, null);
        if (e != null) {
            throw e;
        }

    }

    public static final class Builder {

        @FunctionalInterface
        interface BuildProcessor<T, X extends Throwable> {
            T process(IoTConfig config, IoTProject project) throws X;
        }

        private final IoTProjectBuilder project;
        private final List<PreDeployProcessor> preDeploy = new LinkedList<>();

        private IoTConfigBuilder config;

        private Consumer<Throwable> exceptionHandler = IoTTestSession::defaultExceptionHandler;

        private Builder(final IoTConfigBuilder config, final IoTProjectBuilder project) {
            this.config = config;
            this.project = project;
        }

        public Builder config(final ThrowingConsumer<IoTConfigBuilder> configCustomizer) throws Exception {
            configCustomizer.accept(this.config);
            return this;
        }

        public Builder project(final ThrowingConsumer<IoTProjectBuilder> projectCustomizer) throws Exception {
            projectCustomizer.accept(this.project);
            return this;
        }

        public Builder preDeploy(final PreDeployProcessor processor) {
            this.preDeploy.add(processor);
            return this;
        }

        public Builder adapters(final EnumSet<Adapter> enable) {

            for (var adapter : EnumSet.complementOf(enable)) {
                this.config = adapter.disable(this.config);
            }

            for (var adapter : enable) {
                this.config = adapter.enable(this.config);
            }

            return this;

        }

        public Builder exceptionHandler(final Consumer<Throwable> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * Ensure only the provided adapters are enabled.
         *
         * @param adapters The adapter to enable.
         */
        public Builder adapters(final Adapter... adapters) {
            return adapters(EnumSet.copyOf(Arrays.asList(adapters)));
        }

        /**
         * Deploy the setup, run the code and clean up.
         *
         * @param code The code to run.
         * @throws Exception if anything goes wrong.
         */
        public void run(final Code code) throws Exception {
            try (IoTTestSession session = deploy()) {
                session.run(code);
            }
        }

        public interface PreDeployContext {
            void addCleanup(ThrowingCallable cleanup);
        }

        @FunctionalInterface
        public interface PreDeployProcessor {
            void preDeploy(PreDeployContext context, IoTConfigBuilder config, IoTProjectBuilder project) throws Exception;
        }

        /**
         * Deploy the test session infrastructure.
         *
         * @return The test session for further processing.
         * @throws Exception in case the deployment fails.
         */
        public IoTTestSession deploy() throws Exception {

            // stuff to clean up

            final List<ThrowingCallable> cleanup = new LinkedList<>();

            try {

                // pre deploy

                for (final PreDeployProcessor processor : this.preDeploy) {
                    processor.preDeploy(new PreDeployContext() {
                        @Override
                        public void addCleanup(final ThrowingCallable cleanupTask) {
                            cleanup.add(cleanupTask);
                        }
                    }, this.config, this.project);
                }

                // build objects

                var config = this.config.build();
                var project = this.project.build();

                /*
                 * Create resources: in order to properly clean up, register cleanups first, then perform the operation
                 */

                var infraNamespace = config.getMetadata().getNamespace();

                // create device manager role, we do not clean it up

                DeviceManagementApi.createManagementServiceAccount(infraNamespace);

                // deploy certificates, we do not clean them up

                deployDefaultCerts(infraNamespace);

                // create messaging infrastructure

                var messagingInfrastructure = new MessagingInfrastructureBuilder()
                        .withNewMetadata()
                        .withNamespace(infraNamespace)
                        .withName("default")
                        .endMetadata()

                        .withNewSpec()
                        .withNewBroker().endBroker()
                        .withNewRouter().endRouter()
                        .endSpec()

                        .build();
                createDefaultResource(Kubernetes::messagingInfrastructures, messagingInfrastructure, cleanup);

                // create IoT config

                final Vertx vertx = Vertx.factory.vertx();
                cleanup.add(vertx::close);

                if (!Environment.getInstance().skipCleanup()) {
                    cleanup.add(() -> IoTUtils.deleteIoTConfigAndWait(config));
                }
                IoTUtils.createIoTConfig(config);

                // the project namespace

                var namespace = project.getMetadata().getNamespace();

                // create namespace if not created

                if (!Environment.getInstance().skipCleanup()) {
                    cleanup.add(() -> Kubernetes.getInstance().deleteNamespace(namespace));
                }
                Kubernetes.getInstance().createNamespace(namespace);

                // create messaging project

                var messagingProject = new MessagingProjectBuilder()
                        .withNewMetadata()
                        .withNamespace(namespace)
                        .withName("default")
                        .endMetadata()

                        .withNewSpec().endSpec()

                        .build();
                createDefaultResource(Kubernetes::messagingProjects, messagingProject, cleanup);

                // create messaging endpoint

                var messagingEndpoint = new MessagingEndpointBuilder()
                        .withNewMetadata()
                        .withNamespace(namespace)
                        .withName("downstream")
                        .endMetadata()

                        .withNewSpec()
                        .withHost(Kubernetes.getInstance().getHost())
                        .withNewNodePort().endNodePort()
                        .addNewProtocol("AMQP")
                        .endSpec()

                        .build();
                messagingEndpoint = createDefaultResource(Kubernetes::messagingEndpoints, messagingEndpoint, cleanup);
                var endpointHost = messagingEndpoint.getStatus().getHost();

                // create IoT project

                if (!Environment.getInstance().skipCleanup()) {
                    cleanup.add(() -> IoTUtils.deleteIoTProjectAndWait(project));
                }
                IoTUtils.createIoTProject(project);

                // create endpoints

                final Endpoint deviceRegistryEndpoint = IoTUtils.getDeviceRegistryManagementEndpoint();
                final DeviceRegistryClient registryClient = new DeviceRegistryClient(vertx, deviceRegistryEndpoint);
                cleanup.add(registryClient::close);
                final CredentialsRegistryClient credentialsClient = new CredentialsRegistryClient(vertx, deviceRegistryEndpoint);
                cleanup.add(credentialsClient::close);

                // create user

                // FIXME: when users are back, we need to create a user for telemetry, events, and command and control.
                /*

                var tenantId = getTenantId(project);

                final AddressSpace addressSpace = Kubernetes.getInstance().getAddressSpaceClient().inNamespace(project.getMetadata().getNamespace())
                        .withName(project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName()).get();

                final UserCredentials credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
                final User user = UserUtils.createUserResource(credentials)

                        .editOrNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(addressSpace.getMetadata().getName() + "." + credentials.getUsername())
                        .endMetadata()

                        .editSpec()
                        .withAuthorization(
                                Collections.singletonList(new UserAuthorizationBuilder()
                                        .withAddresses(
                                                "telemetry" + "/" + tenantId,
                                                "telemetry" + "/" + tenantId + "/*",
                                                "event" + "/" + tenantId,
                                                "event" + "/" + tenantId + "/*")
                                        .withOperations(Operation.recv)
                                        .build()))
                        .endSpec()
                        .done();

                Kubernetes.getInstance().getUserClient().inNamespace(project.getMetadata().getNamespace())
                        .createOrReplace(user);
                if (!Environment.getInstance().skipCleanup()) {
                    cleanup.add(
                            () -> Kubernetes.getInstance()
                                    .getUserClient().inNamespace(project.getMetadata().getNamespace())
                                    .withName(user.getMetadata().getName())
                                    .delete());
                }

                UserUtils.waitForUserActive(user, ofDuration(ofMinutes(1)));
                */

                // eval messaging endpoint

                var port = messagingEndpoint.getStatus()
                        .getPorts().stream()
                        .filter(p -> "AMQP".equals(p.getProtocol()))
                        .map(MessagingEndpointPort::getPort)
                        .findAny().orElseThrow(() -> new IllegalStateException("Unable to find port 'amqp' in endpoint status"));
                final Endpoint amqpEndpoint = new Endpoint(endpointHost, port);

                // create AMQP client

                AmqpClient client = new AmqpClient(defaultQueue(amqpEndpoint));
                cleanup.add(client::close);

                // done

                return new IoTTestSession(vertx, config, project, registryClient, credentialsClient, client, this.exceptionHandler, cleanup);

                // the method returned and the IoTTestSession took ownership of the "cleanup" list.

            } catch (Throwable e) {

                if (log.isDebugEnabled()) {
                    log.debug("Caught exception during deployment", e);
                } else {
                    log.info("Caught exception during deployment, running exception handler", e);
                }

                // first run exception handler
                try {
                    this.exceptionHandler.accept(e);
                } catch (Exception e2) {
                    log.info("Failed to run exception handler", e2);
                    e.addSuppressed(new RuntimeException("Failed to run exception handler", e2));
                }

                // cleanup in case any creation step failed
                throw cleanup(cleanup, e);

            }
        }

    }

    /**
     * Create a new test session builder.
     *
     * @param infraNamespace The namespace for the IoTConfig.
     * @param isOpenshiftFour Create configuration for OCP4.
     * @return The new instance.
     */
    public static IoTTestSession.Builder create(final String infraNamespace, final boolean isOpenshiftFour) {

        // create new default IoT infrastructure

        var config = createDefaultConfig(infraNamespace, isOpenshiftFour);

        // we use the same name for the IoTProject and the AddressSpace

        var name = UUID.randomUUID().toString();

        // create new default project setup

        var project = new IoTProjectBuilder(createDefaultTenant(IOT_PROJECT_NAMESPACE, name));

        // done

        return new Builder(config, project);
    }

    /**
     * Create a basic test session builder.
     * <p>
     * If you want a ready-to-run configuration, use {@link #createDefault()}.
     *
     * @return The new builder, missing services and undeployed.
     */
    public static IoTTestSession.Builder create() {
        return create(
                Kubernetes.getInstance().getInfraNamespace(),
                isOpenShiftCompatible(OCP4));
    }

    /**
     * Create a default, ready-to-run, setup.
     *
     * @return The new builder, still undeployed.
     */
    public static IoTTestSession.Builder createDefault() {
        return create()
                .preDeploy(withDefaultServices());
    }

    /**
     * Create a new default config, using the default namespace.
     * <p>
     * The default namespace is evaluated by a call to
     * {@link Kubernetes#getInfraNamespace()}, which requires an active Kubernetes
     * environment.
     */
    public static IoTConfigBuilder createDefaultConfig() {
        return createDefaultConfig(
                Kubernetes.getInstance().getInfraNamespace(),
                isOpenShiftCompatible(OCP4));
    }

    public static IoTConfigBuilder createDefaultConfig(final String namespace, final boolean isOpenshiftFour) {

        var config = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .withNamespace(namespace)
                .endMetadata();

        // enable routes / load balancers by default

        config = config.editOrNewSpec()
                .withEnableDefaultRoutes(true)
                .endSpec();

        // configure logging

        config = config.editOrNewSpec()
                .withNewLogging()
                .withNewLevel("info")
                .addToLoggers("org.eclipse.hono", "debug")
                .addToLoggers("io.enmasse", "debug")
                .endLogging()
                .endSpec();

        if (isOpenshiftFour) {

            // enable service CA by default

            config = config
                    .editOrNewSpec()
                    .withNewInterServiceCertificates()
                    .withNewServiceCAStrategy()
                    .endServiceCAStrategy()
                    .endInterServiceCertificates()
                    .endSpec();

            // switch to provided key/cert for adapters that needs it
            // MQTT: needs passthrough routes because MQTT is not HTTP
            // HTTP: needs passthrough routes because X.509 client certs are being used

            config = useSystemtestKeys(config, HTTP, MQTT, AMQP);

        } else {

            // fall back to manual secrets for inter-service communication

            final Map<String, String> secrets = new HashMap<>();
            secrets.put("iot-auth-service", "systemtests-iot-auth-service-tls");
            secrets.put("iot-tenant-service", "systemtests-iot-tenant-service-tls");
            secrets.put("iot-device-connection", "systemtests-iot-device-connection-tls");
            secrets.put("iot-device-registry", "systemtests-iot-device-registry-tls");
            secrets.put("iot-mesh-inter", "systemtests-iot-mesh-inter-tls");
            secrets.put("iot-command-mesh", "systemtests-iot-command-mesh-tls");

            config = config
                    .editOrNewSpec()
                    .withNewInterServiceCertificates()
                    .withNewSecretCertificatesStrategy()
                    .withCaSecretName("systemtests-iot-service-ca")
                    .withServiceSecretNames(secrets)
                    .endSecretCertificatesStrategy()
                    .endInterServiceCertificates()
                    .endSpec();

            // all adapters need explicit endpoint key/certs

            config = useSystemtestKeys(config, Adapter.values());

        }

        return config;
    }

    /**
     * Create a default IoT tenant object.
     *
     * <strong>Note:</strong> This method only created the instance of the Java object. It does not yet create the
     * instance in Kubernetes.
     *
     * @param namespace The namespace the object should be in.
     * @param name The name of the object
     * @return The new project instance, ready to be created.
     */
    public static IoTProject createDefaultTenant(final String namespace, final String name) {
        return new IoTProjectBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();
    }

    private static IoTConfigBuilder useSystemtestKeys(IoTConfigBuilder config, final Adapter... adapters) {
        for (Adapter adapter : adapters) {
            config = adapter.edit(config, c -> c
                    .editOrNewEndpoint()
                    .withNewSecretNameStrategy("systemtests-iot-" + adapter.name().toLowerCase() + "-adapter-tls")
                    .endEndpoint());
        }
        return config;
    }

    private static Exception cleanup(final List<ThrowingCallable> cleanup, Throwable initialException) {

        if (!Environment.getInstance().skipCleanup()) {

            log.info("Cleaning up resources...");

            for (ThrowingCallable f : Lists.reverse(cleanup)) {
                try {
                    f.call();
                } catch (Throwable e) {
                    if (initialException == null) {
                        initialException = e;
                    } else {
                        initialException.addSuppressed(e);
                    }
                }
            }

            log.info("Cleaning up resources... done!");

        } else {

            log.warn("Skipping resource cleanup!");

        }

        if (initialException == null || initialException instanceof Exception) {
            // return the Exception (or null)
            return (Exception) initialException;
        } else {
            // return Throwable wrapped in exception
            return new Exception(initialException);
        }
    }

    /**
     * Start creating a new device.
     *
     * @param deviceId The ID of the device to create.
     * @return The new device creation instance. The device will only be created when the
     * {@link Device#register()} method is being called.
     */
    public Device newDevice(final String deviceId) {
        return new Device(deviceId);
    }

    public Device newDevice() {
        return newDevice(TestUtils.randomCharacters(23 /* max MQTT client ID length */));
    }

    public Device registerNewRandomDeviceWithPassword() throws Exception {
        return newDevice()
                .register()
                .setPassword(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    /**
     * Add some code to the cleanup handler.
     *
     * @param cleanup The code to call.
     */
    public void addCleanup(final ThrowingCallable cleanup) {
        this.cleanup.add(cleanup);
    }

    protected static void defaultExceptionHandler(final Throwable error) {

        if (Environment.getInstance().isSkipSaveState()) {
            return;
        }

        var test = TestPlanInfo.getInstance().getActualTest();
        if (test == null) {
            test = TestPlanInfo.getInstance().getActualTestClass();
        }

        if (test != null) {
            GlobalLogCollector.saveInfraState(TestUtils.getFailedTestLogsPath(test));
        } else {
            log.error("Unable to log system test failure, no TestInfo details", error);
        }

    }

    /**
     * Add a pre-deploy step to deploy the default service configuration.
     * <p>
     * This will effectively call {@link DefaultDeviceRegistry#newDefaultInstance()} and schedule a call
     * to {@link DefaultDeviceRegistry#deleteDefaultServer()} for cleanup.
     */
    public static PreDeployProcessor withDefaultServices() {
        return (context, config, project) -> {
            try {

                if (!Environment.getInstance().isSkipDeployPostgresql()) {

                    if (!Environment.getInstance().skipUninstall()) {
                        context.addCleanup(DefaultDeviceRegistry::deleteDefaultServer);
                    }

                    config
                            .editOrNewSpec()
                            .withServices(DefaultDeviceRegistry.newDefaultInstance())
                            .endSpec();
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to deploy device registry backend", e);
            }
        };
    }

    private static void deployDefaultCerts(final String namespace) {

        final Path examplesIoT = Paths.get(Environment.getInstance().getTemplatesPath())
                .resolve("install/components/iot/examples");

        if (!Files.isRegularFile(examplesIoT.resolve("k8s-tls/build/root-cert.pem"))) {
            Exec.executeAndCheck(examplesIoT.resolve("k8s-tls/create").toAbsolutePath().toString());
        }
        // deploy will try to undeploy first, so it can always be called
        Exec.executeAndCheck(
                singletonList(examplesIoT.resolve("k8s-tls/deploy").toAbsolutePath().toString()),
                60_000, true, true,
                Map.of(
                        "CLI", KubeCMDClient.getCMD(),
                        "PREFIX", "systemtests-",
                        "NAMESPACE", namespace));
    }

    private static <T extends HasMetadata, L, D> T createDefaultResource(
            final Function<String, MixedOperation<T, L, D, Resource<T, D>>> clientProvider,
            final T resource,
            final List<ThrowingCallable> cleanup) {
        log.info("Creating {}/{}: {}/{}", resource.getApiVersion(), resource.getKind(), resource.getMetadata().getNamespace(), resource.getMetadata().getName());
        var client = clientProvider.apply(resource.getMetadata().getNamespace());
        if (shouldCleanup()) {
            cleanup.add(() -> client.delete(Collections.singletonList(resource)));
        }
        client.create(resource);
        waitUntilConditionOrFail(condition(client, resource, "Ready"), ofMinutes(5), ofSeconds(5));
        log.info("Creating successful. Resource is ready.");
        return client.withName(resource.getMetadata().getName()).get();
    }

    /**
     * Check if created resources should be cleaned up after the test.
     *
     * <strong>Note:</strong> In-memory resources, like communication clients, must always be cleaned up. This check is
     * only intended for persisted resources, so that the current situation can be inspected on a test failure.
     *
     * @return {@code true} if resources should be cleaned up, {@code false} otherwise.
     */
    private static boolean shouldCleanup() {
        return !Environment.getInstance().skipCleanup();
    }
}
