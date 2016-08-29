package enmasse.storage.controller.model;

import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.volume.VolumeType;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * @author lulf
 */
public class FlavorConfig {
    private final DockerImageURI brokerImage;
    private final Set<Port> brokerPorts;
    private final StorageConfig storageConfig;

    private final DockerImageURI routerImage;
    private final Set<Port> routerPorts;
    private final String routerSecretName;
    private final String routerSecretPath;
    private final boolean isShared;
    private final Optional<String> shutdownHook;


    private FlavorConfig(DockerImageURI brokerImage,
                         Set<Port> brokerPorts,
                         StorageConfig storageConfig,
                         DockerImageURI routerImage,
                         Set<Port> routerPorts,
                         String routerSecretName,
                         String routerSecretPath,
                         boolean isShared,
                         Optional<String> shutdownHook) {
        this.brokerImage = brokerImage;
        this.brokerPorts = brokerPorts;
        this.storageConfig = storageConfig;
        this.routerImage = routerImage;
        this.routerPorts = routerPorts;
        this.routerSecretName = routerSecretName;
        this.routerSecretPath = routerSecretPath;
        this.isShared = isShared;
        this.shutdownHook = shutdownHook;
    }

    public Set<Port> brokerPorts() {
        return this.brokerPorts;
    }

    public DockerImageURI brokerImage() {
        return this.brokerImage;
    }

    public StorageConfig storageConfig() {
        return this.storageConfig;
    }

    public boolean isShared() {
        return isShared;
    }

    public Set<Port> routerPorts() {
        return this.routerPorts;
    }

    public DockerImageURI routerImage() {
        return this.routerImage;
    }

    public String routerSecretName() {
        return this.routerSecretName;
    }

    public String routerSecretPath() {
        return this.routerSecretPath;
    }

    public Optional<String> shutdownHook() {
        return this.shutdownHook;
    }

    public static class Builder {
        private DockerImageURI brokerImage = new DockerImageURI("enmasseproject/artemis:latest");
        private Set<Port> brokerPorts = Collections.singleton(new Port("amqp", 5673));
        private StorageConfig storage = new StorageConfig(VolumeType.EMPTY_DIR, "1Gi", "/var/run/artemis");
        private boolean isShared = false;

        private DockerImageURI routerImage = new DockerImageURI("gordons/qdrouterd:latest");
        private Set<Port> routerPorts = Collections.singleton(new Port("amqp", 5672));
        private String routerSecretName;
        private String routerSecretPath;
        private Optional<String> shutdownHook = Optional.empty();

        public Builder brokerImage(DockerImageURI brokerImage) {
            this.brokerImage = brokerImage;
            return this;
        }

        public Builder brokerPorts(Set<Port> brokerPorts) {
            this.brokerPorts = brokerPorts;
            return this;
        }

        public Builder routerImage(DockerImageURI routerImage) {
            this.routerImage = routerImage;
            return this;
        }

        public Builder routerPorts(Set<Port> routerPorts) {
            this.routerPorts = routerPorts;
            return this;
        }

        public Builder routerSecretName(String name) {
            this.routerSecretName = name;
            return this;
        }

        public Builder routerSecretPath(String path) {
            this.routerSecretPath = path;
            return this;
        }

        public Builder shutdownHook(String shutdownHook) {
            this.shutdownHook = Optional.of(shutdownHook);
            return this;
        }

        public FlavorConfig build() {
            return new FlavorConfig(brokerImage, brokerPorts, storage, routerImage, routerPorts, routerSecretName, routerSecretPath, isShared, shutdownHook);
        }

        public Builder storage(StorageConfig storageConfig) {
            this.storage = storageConfig;
            return this;
        }

        public Builder isShared(boolean isShared) {
            this.isShared = isShared;
            return this;
        }
    }
}
