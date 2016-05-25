package amqp;

import io.vertx.proton.ProtonMessageHandler;
import quilt.config.subscription.service.amqp.AMQPServer;
import quilt.config.subscription.service.model.ConfigMapDatabase;
import quilt.config.subscription.service.model.ConfigSubscriber;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author lulf
 */
public class AMQPServerTest {
    private AMQPServer server;
    private ConfigMapDatabase database;
    private TestClient client;

    @Before
    public void setup() throws InterruptedException {
        database = mock(ConfigMapDatabase.class);
        server = new AMQPServer("localhost", 0, database);
        server.run();
        int port = waitForPort(server);
        System.out.println("Server running on port " + server.port());
        client = new TestClient("localhost", port);
    }

    private int waitForPort(AMQPServer server) throws InterruptedException {
        int port = server.port();
        while (port == 0) {
            Thread.sleep(100);
            port = server.port();
        }
        return port;
    }

    @After
    public void teardown() {
        client.close();
        server.close();
    }

    @Test
    public void testSubscribe() {
        ProtonMessageHandler msgHandler = mock(ProtonMessageHandler.class);
        client.subscribe("testconfig", msgHandler);

        ArgumentCaptor<ConfigSubscriber> subCapture = ArgumentCaptor.forClass(ConfigSubscriber.class);
        verify(database, timeout(10000)).subscribe(eq("testconfig"), subCapture.capture());

        Map<String, String> testMap = new LinkedHashMap<>();
        testMap.put("foo", "bar");
        testMap.put("myjson", "{\"hello\":\"world\"}");

        ConfigSubscriber sub = subCapture.getValue();
        sub.configUpdated("testconfig", "1234", testMap);

        ArgumentCaptor<Message> msgCapture = ArgumentCaptor.forClass(Message.class);
        verify(msgHandler, timeout(10000)).handle(any(), msgCapture.capture());
        String value = (String) ((AmqpValue)msgCapture.getValue().getBody()).getValue();
        assertThat(value, is("{\"foo\":\"bar\",\"myjson\":{\"hello\":\"world\"}}"));
    }
}

