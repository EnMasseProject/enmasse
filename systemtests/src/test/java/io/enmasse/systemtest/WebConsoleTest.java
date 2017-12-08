package io.enmasse.systemtest;


import io.enmasse.systemtest.web.FilterType;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public abstract class WebConsoleTest extends SeleniumTestBase {

    public void doTestCreateDeleteAddress(Destination destination) throws Exception {
        createAddressWebConsole(destination);
        deleteAddressWebConsole(destination);
    }

    public void doTestFilterAddressesByType() throws Exception {
        int addressCount = 20;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        createAddressesWebConsole(addresses.toArray(new Destination[0]));
        assertThat(getAddressItems().size(), is(addressCount));
        addFilter(FilterType.TYPE, "queue");

        assertThat(getAddressItems().size(), is(addressCount / 2));

//        Create 10 queues: queue-via-web-N where N=1,3,5,7,9,11,13,15,17,19…​
//
//        Create 10 topics: topic-via-web-M where M=0,2,4,6,8,10,12,14,16,18…​
//
//        Filter all queues and verify that 10 items is visible
//
//        Filter all topics and verify that 10 items is visible
//
//        Remove addresses (shut be done by Tear down


    }

    public void doTestFilterAddressesByName() throws Exception {
        int addressCount = 20;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        createAddressesWebConsole(addresses.toArray(new Destination[0]));

        switchFilter("name");
        WebElement filterInput = getFilterGroup().findElement(By.tagName("input"));
        fillInputItem(filterInput, "web");
        pressEnter(filterInput);
        assertEquals(addressCount, getAddressItems().size());

        fillInputItem(filterInput, "via");
        pressEnter(filterInput);
        assertEquals(addressCount, getAddressItems().size());

        removeFilterByName("web");
        assertEquals(addressCount, getAddressItems().size());

        fillInputItem(filterInput, "queue");
        pressEnter(filterInput);
        assertEquals(addressCount/2, getAddressItems().size());

        clearAllFilters();
        assertEquals(addressCount, getAddressItems().size());
    }
}
