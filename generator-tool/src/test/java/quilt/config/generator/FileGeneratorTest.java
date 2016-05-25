package quilt.config.generator;

import com.openshift.internal.util.Assert;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import quilt.config.generator.ConfigGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
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

        File addr1 = findFile(outputFiles, "broker_cluster_addr1.json").get();
        assertNotNull(addr1);

        File addr2 = findFile(outputFiles, "broker_cluster_addr2.json").get();
        assertNotNull(addr1);
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
