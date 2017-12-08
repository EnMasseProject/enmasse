package io.enmasse.systemtest;


import java.util.ArrayList;
import java.util.stream.IntStream;

public abstract class WebConsoleTest extends SeleniumTestBase {

    public void doTestCreateDeleteAddress(Destination destination) throws Exception {
        createAddressWebConsole(destination);
        deleteAddressWebConsole(destination);
    }

    public void doTestFilterAddressesByType() throws Exception {
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, 20));

        createAddressesWebConsole(addresses.toArray(new Destination[0]));
        deleteAddressesWebConsole(addresses.toArray(new Destination[0]));

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
}
