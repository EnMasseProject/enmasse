/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.bases.iot.ITestIoTBase.IOT_ADDRESS_EVENT;
import static io.enmasse.systemtest.bases.iot.ITestIoTBase.IOT_ADDRESS_TELEMETRY;
import static io.enmasse.systemtest.condition.OpenShiftVersion.OCP4;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.MQTT;
import static io.enmasse.systemtest.platform.Kubernetes.isOpenShiftCompatible;
import static io.enmasse.systemtest.time.TimeoutBudget.ofDuration;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Lists;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.AdapterConfigFluent;
import io.enmasse.iot.model.v1.AdaptersConfigFluent;
import io.enmasse.iot.model.v1.AdaptersConfigFluent.HttpNested;
import io.enmasse.iot.model.v1.AdaptersConfigFluent.LoraWanNested;
import io.enmasse.iot.model.v1.AdaptersConfigFluent.MqttNested;
import io.enmasse.iot.model.v1.AdaptersConfigFluent.SigfoxNested;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTConfigFluent.SpecNested;
import io.enmasse.iot.model.v1.IoTConfigSpecFluent.AdaptersNested;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.iot.ITestIoTBase;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.iot.IoTTestSession.Builder.PreDeployProcessor;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.TestUtils.ThrowingCallable;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;

public final class IoTTestSession implements AutoCloseable {

    public static enum Adapter {
        HTTP {
            @Override
            public IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer) {
                return Adapter.editAdapter(config, AdaptersConfigFluent::editOrNewHttp, HttpNested::endHttp, a -> {
                    consumer.accept(a);
                    return a;
                });
            }
        },
        MQTT {
            @Override
            public IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer) {
                return Adapter.editAdapter(config, AdaptersConfigFluent::editOrNewMqtt, MqttNested::endMqtt, a -> {
                    consumer.accept(a);
                    return a;
                });
            }
        },
        SIGFOX {
            @Override
            public IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer) {
                return Adapter.editAdapter(config, AdaptersConfigFluent::editOrNewSigfox, SigfoxNested::endSigfox, a -> {
                    consumer.accept(a);
                    return a;
                });
            }
        },
        LORAWAN {
            @Override
            public IoTConfigBuilder edit(IoTConfigBuilder config, Consumer<? super AdapterConfigFluent<?>> consumer) {
                return Adapter.editAdapter(config, AdaptersConfigFluent::editOrNewLoraWan, LoraWanNested::endLoraWan, a -> {
                    consumer.accept(a);
                    return a;
                });
            }
        },;

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

        public abstract IoTConfigBuilder edit(final IoTConfigBuilder config, final Consumer<? super AdapterConfigFluent<?>> consumer);

        public IoTConfigBuilder enable(final IoTConfigBuilder config, boolean enabled) {
            return edit(config, a -> a.withEnabled(enabled));
        }

        public IoTConfigBuilder enable(final IoTConfigBuilder config) {
            return enable(config, true);
        }

        public IoTConfigBuilder disable(final IoTConfigBuilder config) {
            return enable(config, false);
        }

    }

    @FunctionalInterface
    public interface Code {
        public void run(IoTTestSession session) throws Exception;
    }

    public class Device {

        private String deviceId;
        private String authId;
        private String password;

        public Device(String deviceId) {
            this.deviceId = deviceId;
        }

        public Device register() throws Exception {
            IoTTestSession.this.registryClient.registerDevice(getTenantId(), this.deviceId);
            return this;
        }

        public Device setPassword(final String authId, final String password) throws Exception {
            this.authId = authId;
            this.password = password;

            var pwd = CredentialsRegistryClient.createPlainPasswordCredentialsObject(authId, password, null);
            IoTTestSession.this.credentialsClient.setCredentials(getTenantId(), this.deviceId, singletonList(pwd));
            return this;
        }

        /**
         * A new http adapter client.
         *
         * @return The new instance. It will automatically be closed when the test session is being cleaned
         *         up.
         */
        public HttpAdapterClient createHttpAdapterClient() {
            return IoTTestSession.this.createHttpAdapterClient(this.authId, this.password);
        }

        /**
         * A new mqtt adapter client.
         *
         * @return The new instance. It will automatically be closed when the test session is being cleaned
         *         up.
         */
        public MqttAdapterClient createMqttAdapterClient() throws Exception {
            return IoTTestSession.this.createMqttAdapterClient(this.deviceId, this.authId, this.password);
        }

        public String getTenantId() {
            return IoTTestSession.this.getTenantId();
        }
    }

    private final IoTConfig config;
    private final IoTProject project;
    private List<ThrowingCallable> cleanup;
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;
    private AmqpClient consumerClient;

    private IoTTestSession(
            final IoTConfig config,
            final IoTProject project,
            final DeviceRegistryClient registryClient,
            final CredentialsRegistryClient credentialsClient,
            final AmqpClient consumerClient,
            final List<ThrowingCallable> cleanup) {

        this.config = config;
        this.project = project;

        this.registryClient = registryClient;
        this.credentialsClient = credentialsClient;

        this.consumerClient = consumerClient;

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

    private HttpAdapterClient createHttpAdapterClient(final String authId, final String password) {

        var endpoint = Kubernetes.getInstance().getExternalEndpoint("iot-http-adapter");
        var result = new HttpAdapterClient(endpoint, authId, getTenantId(), password);
        this.cleanup.add(() -> result.close());

        return result;

    }

    private MqttAdapterClient createMqttAdapterClient(final String deviceId, final String authId, final String password) throws Exception {

        var endpoint = Kubernetes.getInstance().getExternalEndpoint("iot-mqtt-adapter");
        var result = MqttAdapterClient.create(endpoint, deviceId, authId, getTenantId(), password);
        this.cleanup.add(() -> result.close());

        return result;

    }

    public AmqpClient getConsumerClient() {
        return this.consumerClient;
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
            public T process(IoTConfig config, IoTProject project) throws X;
        }

        private IoTConfigBuilder config;
        private IoTProjectBuilder project;

        private Consumer<Throwable> exceptionHandler = IoTTestSession::defaultExceptionHandler;
        private List<PreDeployProcessor> preDeploy = new LinkedList<>();

        private Builder(final IoTConfigBuilder config, final IoTProjectBuilder project) {
            this.config = config;
            this.project = project;
        }

        public Builder config(final Consumer<IoTConfigBuilder> configCustomizer) {
            configCustomizer.accept(this.config);
            return this;
        }

        public Builder project(final Consumer<IoTProjectBuilder> projectCustomizer) {
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

        public void run(Code code) throws Exception {
            try (IoTTestSession session = deploy()) {
                /*
                 * We need an inner try-catch in order to only handle the exception of the test code.
                 * The exception of the deploy method is handled by the deploy method itself.
                 */
                try {
                    code.run(session);
                } catch (Exception e) {
                    this.exceptionHandler.accept(e);
                    throw e;
                }
            }
        }

        public interface PreDeployContext {
            public void addCleanup(ThrowingCallable cleanup);
        }

        @FunctionalInterface
        public interface PreDeployProcessor {
            public void preDeploy(PreDeployContext context, IoTConfigBuilder config, IoTProjectBuilder project) throws Exception;
        }

        /**
         * Deploy the test session infrastructure.
         *
         * @return The test session for further processing.
         * @throws Exception in case the deployment fails.
         */
        private IoTTestSession deploy() throws Exception {

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

                var config = this.config.build();
                var project = this.project.build();

                // create resources

                IoTUtils.createIoTConfig(config);
                if (!Environment.getInstance().skipCleanup()) {
                    cleanup.add(() -> IoTUtils.deleteIoTConfigAndWait(Kubernetes.getInstance(), config));
                }
                IoTUtils.createIoTProject(project);
                if (!Environment.getInstance().skipCleanup()) {
                    cleanup.add(() -> IoTUtils.deleteIoTProjectAndWait(Kubernetes.getInstance(), project));
                }

                final Endpoint deviceRegistryEndpoint = Kubernetes.getInstance().getExternalEndpoint("device-registry");
                final DeviceRegistryClient registryClient = new DeviceRegistryClient(deviceRegistryEndpoint);
                cleanup.add(() -> registryClient.close());
                final CredentialsRegistryClient credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint);
                cleanup.add(() -> credentialsClient.close());

                // create user

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
                                                IOT_ADDRESS_TELEMETRY + "/" + tenantId,
                                                IOT_ADDRESS_TELEMETRY + "/" + tenantId + "/*",
                                                IOT_ADDRESS_EVENT + "/" + tenantId,
                                                IOT_ADDRESS_EVENT + "/" + tenantId + "/*")
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

                AmqpClientFactory clientFactory = new AmqpClientFactory(addressSpace, credentials);
                cleanup.add(() -> clientFactory.close());
                AmqpClient client = clientFactory.createQueueClient();
                cleanup.add(() -> client.close());

                // done

                return new IoTTestSession(config, project, registryClient, credentialsClient, client, cleanup);

            } catch (Exception e) {

                // first run exception handler
                try {
                    this.exceptionHandler.accept(e);
                } catch (Exception e2) {
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
     * @param namespace The namespace for the IoTConfig.
     * @return The new instance.
     * @throws Exception
     */
    public static IoTTestSession.Builder create(final String namespace) {

        // create new default IoT infrastructure

        var config = createDefaultConfig(namespace);

        // we use the same name for the IoTProject and the AddressSpace

        var name = UUID.randomUUID().toString();

        // create new default project setup

        var project = new IoTProjectBuilder(
                IoTUtils.getBasicIoTProjectObject(
                        name, name,
                        ITestIoTBase.IOT_PROJECT_NAMESPACE,
                        AddressSpacePlans.STANDARD_SMALL));

        // done

        return new Builder(config, project);
    }

    /**
     * Create a new default config, using the default namespace.
     * <p>
     * The default namespace is evaluated by a call to
     * {@link Kubernetes#getInfraNamespace()}, which requires an active Kubernetes
     * environment.
     */
    public static IoTConfigBuilder createDefaultConfig() {
        return createDefaultConfig(Kubernetes.getInstance().getInfraNamespace());
    }

    public static IoTConfigBuilder createDefaultConfig(final String namespace) {
        var config = new IoTConfigBuilder()

                .withNewMetadata()
                .withName("default")
                .withNamespace(namespace)
                .endMetadata();

        if (isOpenShiftCompatible(OCP4)) {

            // enable service CA by default

            config = config
                    .editOrNewSpec()
                    .withNewInterServiceCertificates()
                    .withNewServiceCAStrategy()
                    .endServiceCAStrategy()
                    .endInterServiceCertificates()
                    .endSpec();

            // switch to provided key/cert for adapters that needs it

            for (Adapter adapter : asList(MQTT)) {
                config = adapter.edit(config, c -> c
                        .editOrNewEndpoint()
                        .withNewSecretNameStrategy("systemtests-iot-" + adapter.name().toLowerCase() + "-adapter-tls")
                        .endEndpoint());
            }

        } else {

            // fall back to manual secrets for inter-service communication

            final Map<String, String> secrets = new HashMap<>();
            secrets.put("iot-auth-service", "systemtests-iot-auth-service-tls");
            secrets.put("iot-tenant-service", "systemtests-iot-tenant-service-tls");
            secrets.put("iot-device-connection", "systemtests-iot-device-connection-tls");
            secrets.put("iot-device-registry", "systemtests-iot-device-registry-tls");

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

            for (Adapter adapter : Adapter.values()) {
                config = adapter.edit(config, c -> c
                        .editOrNewEndpoint()
                        .withNewSecretNameStrategy("systemtests-iot-" + adapter.name().toLowerCase() + "-adapter-tls")
                        .endEndpoint());
            }

        }

        return config;
    }

    public static IoTTestSession.Builder createDefault() {
        return create(Kubernetes.getInstance().getInfraNamespace())
                .preDeploy(withDefaultServices());
    }

    private static Exception cleanup(final List<ThrowingCallable> cleanup, Exception initialException) {

        for (ThrowingCallable f : Lists.reverse(cleanup)) {
            try {
                f.call();
            } catch (Exception e) {
                if (initialException == null) {
                    initialException = e;
                } else {
                    initialException.addSuppressed(e);
                }
            }
        }

        return initialException;
    }

    /**
     * Start creating a new device.
     *
     * @param deviceId The ID of the device to create.
     * @return The new device creation instance. The device will only be created when the
     *         {@link Device#register()} method is being called.
     */
    public Device newDevice(String deviceId) {
        return new Device(deviceId);
    }

    protected static void defaultExceptionHandler(final Throwable error) {
        if (Environment.getInstance().isSkipSaveState()) {
            return;
        }

        GlobalLogCollector.saveInfraState(TestUtils.getFailedTestLogsPath(TestInfo.getInstance().getActualTest()));
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
                    config
                            .editOrNewSpec()
                            .withServices(DefaultDeviceRegistry.newDefaultInstance())
                            .endSpec();
                }

                if (!Environment.getInstance().skipUninstall()) {
                    context.addCleanup(() -> DefaultDeviceRegistry.deleteDefaultServer());
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to deploy device registry backend", e);
            }
        };
    }

    public static void deployDefaultCerts() throws Exception {
        if (!Files.isRegularFile(Paths.get("../templates/iot/examples/k8s-tls/build/root-cert.pem"))) {
            Exec.execute("../templates/iot/examples/k8s-tls/create");
        }
        // deploy will try to undeploy first, so it can always be called
        Exec.execute(
                asList("../templates/iot/examples/k8s-tls/deploy"),
                60_000, true, true,
                Map.of(
                        "CLI", KubeCMDClient.getCMD(),
                        "PREFIX", "systemtests-"
                        ));
    }
}
