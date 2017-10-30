package io.enmasse.systemtest.executor.client;

import io.enmasse.systemtest.executor.client.rhea.RheaClientReceiver;
import io.enmasse.systemtest.executor.client.rhea.RheaClientSender;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

//TODO: Remove this file after dev is completed
public class ClientExecutionUnitTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        RheaClientSender sender = new RheaClientSender();
        RheaClientReceiver receiver = new RheaClientReceiver();

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.COUNT, "10");
        arguments.put(Argument.ADDRESS, "example_queue");
        arguments.put(Argument.MSG_CONTENT, "pepa");
        arguments.put(Argument.MSG_PROPERTY, "jarda~30");
        arguments.put(Argument.MSG_PROPERTY, "konina~20");
        arguments.put(Argument.SELECTOR, "jarda = 30");

        sender.setArguments(arguments);
        receiver.setArguments(arguments);

        //async
        Future<Boolean> rs = receiver.runAsync();
        Future<Boolean> rr = sender.runAsync();

        System.out.println(rs.get());
        System.out.println(rr.get());

        //non async
        sender.run();
        receiver.run();

        System.exit(0);
    }
}
