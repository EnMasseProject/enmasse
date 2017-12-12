package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.web.ConsoleWebPage;
import io.enmasse.systemtest.web.ISeleniumProvider;
import io.enmasse.systemtest.web.SeleniumProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.WebDriver;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.stream.IntStream;

public abstract class WebConsoleTest extends MarathonTestBase implements ISeleniumProvider {

    SeleniumProvider selenium = new SeleniumProvider();
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
        Logging.log.info("testCreateDeleteUsersLong start");
        AddressSpace addressSpace = new AddressSpace("test-create-delete-users-brokered",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "standard");
        Logging.log.info("Address space '{}'created", addressSpace);
        createUser(addressSpace, "test", "test");

        selenium.setupDriver(environment, openShift, buildDriver());
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressApiClient, addressSpace);
    }

    @After
    public void tearDownDrivers() throws Exception {
        selenium.tearDownDrivers();
    }

    public void doTestCreateDeleteAddressesViaAgentLong() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        runTestInLoop(30, () -> {
            consoleWebPage.createAddressesWebConsole();
        });
        Logging.log.info("testCreateDeleteUsersLong finished");
    }

    @Override
    public WebDriver buildDriver() {
        throw new NotImplementedException();
    }
}
