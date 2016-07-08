package enmasse.storage.controller.generator;

import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author lulf
 */
public class FileGeneratorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static FileGenerator generator;

    @BeforeClass
    public static void setUpOnce() {
        generator = new FileGenerator();
    }

    @Test
    public void testBrokerGenerator() throws IOException {
        File outputDir = folder.newFolder();
        generator.generate("src/test/resources/example-config.json", outputDir.getAbsolutePath());

        File[] outputFiles = outputDir.listFiles();
        assertThat(outputFiles.length, CoreMatchers.is(2));

        assertClusterExists(outputFiles, "storage_cluster_addr1");
        assertClusterExists(outputFiles, "storage_cluster_addr2");
    }

    private void assertClusterExists(File [] outputFiles, String fileName) {
        File f = findFile(outputFiles, fileName).get();
        assertNotNull(f);
        File[] resources = f.listFiles();
        assertFileExists(resources, "replicationcontroller.json");
    }

    private void assertFileExists(File [] outputFiles, String fileName) {
        File f = findFile(outputFiles, fileName).get();
        assertNotNull(f);
    }

    private Optional<File> findFile(File[] outputFiles, String fileName) {
        for (File file : outputFiles) {
            if (fileName.equals(file.getName())) {
                return Optional.of(file);
            }
        }
        return Optional.empty();
    }
}
