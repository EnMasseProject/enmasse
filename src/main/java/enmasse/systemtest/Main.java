package enmasse.systemtest;

import enmasse.systemtest.mqtt.PublishTest;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

public class Main {
    public static void main(String [] args) throws InterruptedException {
        JUnitCore core = new JUnitCore();
        core.addListener(new XmlListener(outputDir));
        core.addListener(new TextListener(System.out));
        core.run(AnycastTest.class, BroadcastTest.class, QueueTest.class, TopicTest.class, PublishTest.class);
    }
}
