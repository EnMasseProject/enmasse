package amqp;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonMessageHandler;

/**
 * @author lulf
 */
public class TestClient {

    private String serverHost;
    private int serverPort;
    private ProtonClient client;
    private ProtonConnection connection;

    public TestClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.client = ProtonClient.create(Vertx.vertx());
    }

    public void subscribe(String address, ProtonMessageHandler handler) {
        client.connect(new ProtonClientOptions().setConnectTimeout(10000), serverHost, serverPort, connectResult -> {
            if (connectResult.succeeded()) {
                System.out.println("Connected'");
                connection = connectResult.result();
                connection.open();
                System.out.println("Creating receiver");
                connection.createReceiver(address).handler(handler).open();
            } else {
                System.out.println("Connection failed: " + connectResult.cause().getMessage());
            }
        });
    }

    public void close() {
        if (connection != null) {
            connection.close();
        }
    }
}
