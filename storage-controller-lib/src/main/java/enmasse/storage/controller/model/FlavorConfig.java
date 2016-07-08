package enmasse.storage.controller.model;

import com.openshift.internal.restclient.model.volume.VolumeSource;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.volume.VolumeType;

/**
 * @author lulf
 */
public class FlavorConfig { private final DockerImageURI brokerImage;
    private final int brokerPort;
    private final StorageConfig storageConfig;

    private final DockerImageURI routerImage;
    private final int routerPort;
    private final String routerSecretName;
    private final String routerSecretPath;
    private final boolean isShared;


    private FlavorConfig(DockerImageURI brokerImage, int brokerPort, StorageConfig storageConfig, DockerImageURI routerImage, int routerPort,
                         String routerSecretName, String routerSecretPath, boolean isShared) {
        this.brokerImage = brokerImage;
        this.brokerPort = brokerPort;
        this.storageConfig = storageConfig;
        this.routerImage = routerImage;
        this.routerPort = routerPort;
        this.routerSecretName = routerSecretName;
        this.routerSecretPath = routerSecretPath;
        this.isShared = isShared;
    }

    public int brokerPort() {
        return this.brokerPort;
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

    public int routerPort() {
        return this.routerPort;
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

    public static class Builder {
        private DockerImageURI brokerImage = new DockerImageURI("enmasseproject/artemis:latest");
        private int brokerPort = 5673;
        private StorageConfig storage = new StorageConfig(VolumeType.EMPTY_DIR, "1Gi", "/var/run/artemis");
        private boolean isShared = false;

        private DockerImageURI routerImage = new DockerImageURI("gordons/qdrouterd:v9");
        private int routerPort = 5672;
        private String routerSecretName;
        private String routerSecretPath;

        public Builder brokerImage(DockerImageURI brokerImage) {
            this.brokerImage = brokerImage;
            return this;
        }

        public Builder brokerPort(int brokerPort) {
            this.brokerPort = brokerPort;
            return this;
        }

        public Builder routerImage(DockerImageURI routerImage) {
            this.routerImage = routerImage;
            return this;
        }

        public Builder routerPort(int routerPort) {
            this.routerPort = routerPort;
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

        public FlavorConfig build() {
            return new FlavorConfig(brokerImage, brokerPort, storage, routerImage, routerPort, routerSecretName, routerSecretPath, isShared);
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
