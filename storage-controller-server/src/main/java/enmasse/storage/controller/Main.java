package enmasse.storage.controller;

import io.vertx.core.impl.FileResolver;

import java.io.IOException;

/**
 * @author lulf
 */
public class Main {

    public static void main(String args[]) {
        try {
            // Kind of a hack
            System.setProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp/vert.x");

            StorageController service = new StorageController(StorageControllerOptions.fromEnv(System.getenv()));
            service.run();
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error starting storage controller: " + e.getMessage());
            System.exit(1);
        }
    }
}
