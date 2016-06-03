package quilt.config.model;

import com.openshift.restclient.images.DockerImageURI;

import java.util.Collections;
import java.util.List;

/**
 * @author lulf
 */
public class BrokerProperties {
    private final DockerImageURI brokerImage;
    private final int brokerPort;
    private final List<String> brokerMounts;

    private final DockerImageURI routerImage;
    private final int routerPort;


    public BrokerProperties(DockerImageURI brokerImage, int brokerPort, List<String> brokerMounts, DockerImageURI routerImage, int routerPort) {
        this.brokerImage = brokerImage;
        this.brokerPort = brokerPort;
        this.brokerMounts = brokerMounts;
        this.routerImage = routerImage;
        this.routerPort = routerPort;
    }

    public int brokerPort() {
        return this.brokerPort;
    }

    public DockerImageURI brokerImage() {
        return this.brokerImage;
    }

    public List<String> brokerMounts() {
        return this.brokerMounts;
    }

    public int routerPort() {
        return this.routerPort;
    }

    public DockerImageURI routerImage() {
        return this.routerImage;
    }

    public static class Builder {
        private DockerImageURI brokerImage = new DockerImageURI("gordons/artemis:latest");
        private int brokerPort = 5673;
        private List<String> brokerMounts = Collections.singletonList("/var/run/artemis");

        private DockerImageURI routerImage = new DockerImageURI("gordons/qdrouterd:v7");
        private int routerPort = 5672;

        public Builder brokerImage(DockerImageURI brokerImage) {
            this.brokerImage = brokerImage;
            return this;
        }

        public Builder brokerPort(int brokerPort) {
            this.brokerPort = brokerPort;
            return this;
        }

        public Builder brokerMounts(List<String> brokerMounts) {
            this.brokerMounts = brokerMounts;
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

        public BrokerProperties build() {
            return new BrokerProperties(brokerImage, brokerPort, brokerMounts, routerImage, routerPort);
        }
    }
}
