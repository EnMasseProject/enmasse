package enmasse.storage.controller.generator;

import com.openshift.restclient.model.IResource;
import enmasse.storage.controller.model.BrokerProperties;
import enmasse.storage.controller.model.Config;
import enmasse.storage.controller.model.parser.ConfigParser;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author lulf
 */
public class FileGenerator {

    private final ConfigParser parser = new ConfigParser();
    private final StorageGenerator generator = new StorageGenerator(new OpenshiftClient(null, "filegen"), new BrokerProperties.Builder().build());

    private static final String STORAGE_CLUSTER_PATTERN = "storage_cluster_%s";
    private static final String FILE_PATTERN = "%s.json";

    public void generate(String inputFilePath, String outputDirPath) throws IOException {
        File inputFile = new File(inputFilePath);
        File outputDir = new File(outputDirPath);

        try (FileReader reader = new FileReader(inputFile)) {
            Config config = parser.parse(reader);
            generateConfig(config, outputDir);
        }
    }

    private void generateConfig(Config config, File outputDir) throws IOException {
        List<StorageCluster> clusterList = generator.generate(config.destinations());
        for (StorageCluster cluster : clusterList) {
            generateConfig(cluster, outputDir);
        }
    }

    private void generateConfig(StorageCluster cluster, File outputDir) throws IOException {
        File clusterDir = new File(outputDir, String.format(STORAGE_CLUSTER_PATTERN, cluster.getAddress()));
        clusterDir.mkdirs();
        for (IResource resource : cluster.getResources()) {
            writeBrokerConfig(resource, new File(clusterDir, formatFileName(String.format(FILE_PATTERN, resource.getKind().toLowerCase()))));
        }
    }

    private String formatFileName(String name) {
        return name.replaceAll("-", "_");
    }

    private void writeBrokerConfig(IResource resource, File outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(resource.toJson(false));
        }
    }
}
