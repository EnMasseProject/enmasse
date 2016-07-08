package enmasse.storage.controller.model;

/**
 * @author Ulf Lilleengen
 */
public class StorageConfig {
    private final String volumeType;
    private final String size;
    private final String mountPath;

    public StorageConfig(String volumeType, String size, String mountPath) {
        this.volumeType = volumeType;
        this.size = size;
        this.mountPath = mountPath;
    }

    public String volumeType() {
        return volumeType;
    }

    public String size() {
        return size;
    }

    public String mountPath() {
        return mountPath;
    }
}
