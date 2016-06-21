package enmasse.rc.model;

import com.openshift.restclient.images.DockerImageURI;

/**
 * @author lulf
 */
public class BrokerProperties {
    private final DockerImageURI brokerImage;
    private final int brokerPort;
    private final String brokerMountPath;

    private final DockerImageURI routerImage;
    private final int routerPort;
    private final String routerSecretName;
    private final String routerSecretPath;


    public BrokerProperties(DockerImageURI brokerImage, int brokerPort, String brokerMountPath, DockerImageURI routerImage, int routerPort) {
        this(brokerImage, brokerPort, brokerMountPath, routerImage, routerPort, null, null);
    }

    public BrokerProperties(DockerImageURI brokerImage, int brokerPort, String brokerMountPath, DockerImageURI routerImage, int routerPort,
                            String routerSecretName, String routerSecretPath) {
        this.brokerImage = brokerImage;
        this.brokerPort = brokerPort;
        this.brokerMountPath = brokerMountPath;
        this.routerImage = routerImage;
        this.routerPort = routerPort;
        this.routerSecretName = routerSecretName;
        this.routerSecretPath = routerSecretPath;
    }

    public int brokerPort() {
        return this.brokerPort;
    }

    public DockerImageURI brokerImage() {
        return this.brokerImage;
    }

    public String brokerMountPath() {
        return this.brokerMountPath;
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
        private String brokerMountPath = "/var/run/artemis";

        private DockerImageURI routerImage = new DockerImageURI("gordons/qdrouterd:v8");
        private int routerPort = 5672;
        private String routerSecretName = "qdrouterd-certs";
        private String routerSecretPath = "/etc/qpid-dispatch/ssl/";

        public Builder brokerImage(DockerImageURI brokerImage) {
            this.brokerImage = brokerImage;
            return this;
        }

        public Builder brokerPort(int brokerPort) {
            this.brokerPort = brokerPort;
            return this;
        }

        public Builder brokerMountPath(String brokerMountPath) {
            this.brokerMountPath = brokerMountPath;
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

        public BrokerProperties build() {
            return new BrokerProperties(brokerImage, brokerPort, brokerMountPath, routerImage, routerPort, routerSecretName, routerSecretPath);
        }
    }
}
