/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.selenium.ConsoleWebPage;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.stream.IntStream;

public abstract class WebConsoleTest extends MarathonTestBase implements ISeleniumProvider {
    private static Logger log = CustomLogger.getLogger();
    SeleniumProvider selenium = new SeleniumProvider();
    AddressSpace addressSpace;

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            selenium.onFailed(e, description);
        }
    };
    ConsoleWebPage consoleWebPage;

    @Before
    public void setUpWebConsoleTests() throws Exception {
        log.info("testCreateDeleteUsersLong start");
        addressSpace = new AddressSpace("brokered-marathon-web-console",
                AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(addressSpace);
        log.info("Address space '{}'created", addressSpace);

        username = "test";
        password = "test";
        createUser(addressSpace, username, username);

        selenium.setupDriver(environment, kubernetes, buildDriver());
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressApiClient, addressSpace);
    }

    @After
    public void tearDownDrivers() throws Exception {
        selenium.tearDownDrivers();
        deleteAddressSpace(addressSpace);
    }

    public void doTestCreateDeleteAddressesViaAgentLong() throws Exception {
        int addressCount = 5;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        runTestInLoop(30, () -> {
            consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
            consoleWebPage.deleteAddressesWebConsole(addresses.toArray(new Destination[0]));
            Thread.sleep(5000);
        });
        log.info("testCreateDeleteAddressesViaAgentLong finished");
    }

}
