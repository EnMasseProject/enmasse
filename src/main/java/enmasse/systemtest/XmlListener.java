package enmasse.systemtest;

import org.junit.runner.notification.RunListener;

import java.io.File;

public class XmlListener extends RunListener {
    private final File outputDir;
    public XmlListener(File outputDir) {
        this.outputDir = outputDir;
    }
}
