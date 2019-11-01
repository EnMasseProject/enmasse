package io.enmasse.systemtest.utils;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import org.slf4j.Logger;

public class ConsoleUtils {
    private static final Kubernetes KUBERNETES = Kubernetes.getInstance();
    private static final Logger LOGGER = CustomLogger.getLogger();
    /**
     * Gets console route.
     *
     * @param addressSpace the address space
     * @return the console route
     */
    public static String getConsoleRoute(AddressSpace addressSpace) {
        Endpoint consoleEndpoint = getConsoleEndpoint(addressSpace);
        String consoleRoute = String.format("https://%s", consoleEndpoint.toString());
        LOGGER.info(consoleRoute);
        return consoleRoute;
    }

    /**
     * Gets console endpoint.
     *
     * @param addressSpace the address space
     * @return the console endpoint
     */
    private static Endpoint getConsoleEndpoint(AddressSpace addressSpace) {
        Endpoint consoleEndpoint = AddressSpaceUtils.getEndpointByServiceName(addressSpace, "console");
        if (consoleEndpoint == null) {
            String externalEndpointName = AddressSpaceUtils.getExternalEndpointName(addressSpace, "console");
            consoleEndpoint = KUBERNETES.getExternalEndpoint(externalEndpointName);
        }
        return consoleEndpoint;
    }

    /**
     * selenium provider with Firefox webdriver
     */
    public static SeleniumProvider getFirefoxSeleniumProvider() throws Exception {
        SeleniumProvider seleniumProvider = SeleniumProvider.getInstance();
        seleniumProvider.setupDriver(TestUtils.getFirefoxDriver());
        return seleniumProvider;
    }
}
