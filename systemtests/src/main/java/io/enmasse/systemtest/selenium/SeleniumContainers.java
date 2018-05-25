/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;


import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.DockerCmdClient;
import org.slf4j.Logger;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeleniumContainers {

    private static Logger log = CustomLogger.getLogger();
    private static final String FIREFOX_IMAGE = "selenium/standalone-firefox-debug";
    private static final String CHROME_IMAGE = "selenium/standalone-chrome-debug";
    private static final String FIREFOX_CONTAINER_NAME = "selenium-firefox";
    private static final String CHROME_CONTAINER_NAME = "selenium-chrome";

    public static void deployFirefoxContainer() {
        log.info("Deploy firefox container");
        DockerCmdClient.pull("docker.io", FIREFOX_IMAGE, "latest");
        stopAndRemoveFirefoxContainer();
        DockerCmdClient.runContainer(FIREFOX_IMAGE, FIREFOX_CONTAINER_NAME,
                generateSeleniumOpts("4444", ":99"));
        copyRheaWebPageFirefox();
        assertTrue(DockerCmdClient.isContainerRunning(FIREFOX_CONTAINER_NAME));
    }

    public static void deployChromeContainer() {
        log.info("Deploy chrome container");
        DockerCmdClient.pull("docker.io", CHROME_IMAGE, "latest");
        stopAndRemoveChromeContainer();
        DockerCmdClient.runContainer(CHROME_IMAGE, CHROME_CONTAINER_NAME,
                generateSeleniumOpts("4443", ":98"));
        copyRheaWebPageChrome();
        assertTrue(DockerCmdClient.isContainerRunning(CHROME_CONTAINER_NAME));
    }

    public static void stopAndRemoveFirefoxContainer() {
        DockerCmdClient.stopContainer(FIREFOX_CONTAINER_NAME);
        DockerCmdClient.removeContainer(FIREFOX_CONTAINER_NAME);
    }

    public static void stopAndRemoveChromeContainer() {
        DockerCmdClient.stopContainer(CHROME_CONTAINER_NAME);
        DockerCmdClient.removeContainer(CHROME_CONTAINER_NAME);
    }

    private static void copyRheaWebPage(String containerName) {
        File rheaHtml = new File("src/main/resources/rhea.html");
        File rheaJs = new File("client_executable/rhea/dist/rhea.js");

        DockerCmdClient.copyToContainer(containerName, rheaHtml.getAbsolutePath(), "/opt/rhea.html");
        DockerCmdClient.copyToContainer(containerName, rheaJs.getAbsolutePath(), "/opt/rhea.js");
    }

    public static void restartFirefoxContainer() {
        DockerCmdClient.restartContainer(FIREFOX_CONTAINER_NAME);
    }

    public static void restartChromeContainer() {
        DockerCmdClient.restartContainer(CHROME_CONTAINER_NAME);
    }

    public static void copyRheaWebPageChrome() {
        copyRheaWebPage(CHROME_CONTAINER_NAME);
    }

    public static void copyRheaWebPageFirefox() {
        copyRheaWebPage(FIREFOX_CONTAINER_NAME);
    }

    private static String[] generateSeleniumOpts(String port, String display) {
        return Arrays.asList(
                "-d",
                "-p", String.format("%s:%s", port, port),
                "--network", "host",
                "-e", String.format("DISPLAY=%s", display),
                "-e", String.format("SE_OPTS=-port %s", port)).toArray(new String[0]);
    }
}
