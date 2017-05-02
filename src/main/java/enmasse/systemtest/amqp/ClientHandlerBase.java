package enmasse.systemtest.amqp;

import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;

import java.util.concurrent.CompletableFuture;

public abstract class ClientHandlerBase<T> extends AbstractVerticle {

    private final enmasse.systemtest.Endpoint endpoint;
    protected final ClientOptions clientOptions;
    protected final CompletableFuture<T> promise;

    public ClientHandlerBase(enmasse.systemtest.Endpoint endpoint, ClientOptions clientOptions, CompletableFuture<T> promise) {
        this.endpoint = endpoint;
        this.clientOptions = clientOptions;
        this.promise = promise;
    }

    @Override
    public void start() {
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(clientOptions.getProtonClientOptions(), endpoint.getHost(), endpoint.getPort(), connection -> {
            if (connection.succeeded()) {
                ProtonConnection conn = connection.result();
                conn.setContainer("enmasse-systemtest-client");
                conn.openHandler(result -> {
                    if (result.failed()) {
                        promise.completeExceptionally(result.cause());
                    } else {
                        connectionOpened(conn);
                    }
                });
                conn.closeHandler(result -> {
                    if (result.failed()) {
                        promise.completeExceptionally(result.cause());
                    } else {
                        connectionClosed(conn);
                    }
                });
                conn.disconnectHandler(result -> connectionDisconnected(conn));
                conn.open();
            } else {
                System.out.println("Connection to " + endpoint.getHost() + ":" + endpoint.getPort() + " failed: " + connection.cause().getMessage());
                promise.completeExceptionally(connection.cause());
            }
        });
        System.out.println("Client started");
    }

    @Override
    public void stop() {
        System.out.println("Client stopped");
    }

    protected abstract void connectionOpened(ProtonConnection conn);
    protected abstract void connectionClosed(ProtonConnection conn);
    protected abstract void connectionDisconnected(ProtonConnection conn);

    protected void handleError(ErrorCondition error) {
        if (error == null || error.getCondition() == null) {
            System.out.println("Link closed without error");
        } else {
            System.out.println("Link closed with " + error);
            promise.completeExceptionally(new RuntimeException(error.getDescription()));
        }
    }
}
