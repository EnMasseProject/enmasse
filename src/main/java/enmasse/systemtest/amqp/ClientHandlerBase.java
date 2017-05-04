package enmasse.systemtest.amqp;

import enmasse.systemtest.Logging;
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
                Logging.log.info("Connection to " + endpoint.getHost() + ":" + endpoint.getPort() + " failed: " + connection.cause().getMessage());
                promise.completeExceptionally(connection.cause());
            }
        });
    }

    protected abstract void connectionOpened(ProtonConnection conn);
    protected abstract void connectionClosed(ProtonConnection conn);
    protected abstract void connectionDisconnected(ProtonConnection conn);

    protected void handleError(ErrorCondition error) {
        if (error == null || error.getCondition() == null) {
            Logging.log.info("Link closed without error");
        } else {
            Logging.log.info("Link closed with " + error);
            promise.completeExceptionally(new RuntimeException(error.getDescription()));
        }
    }
}
