package quilt.config.subscription.service.amqp;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;
import quilt.config.subscription.service.model.ConfigMapDatabase;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AMQP server endpoint that handles connections to the service and propagates config for a config map specified
 * as the address to which the client wants to receive.
 *
 * @author lulf
 */
public class AMQPServer {
    private static final Logger log = Logger.getLogger(AMQPServer.class.getName());

    private final ConfigMapDatabase database;
    private final ProtonServer server;
    private final String hostname;
    private final int port;

    public AMQPServer(String hostname, int port, ConfigMapDatabase database)
    {
        this.hostname = hostname;
        this.port = port;
        this.database = database;
        this.server = ProtonServer.create(Vertx.vertx());

        server.connectHandler(this::connectHandler);
    }

    private void connectHandler(ProtonConnection connection) {
        connection.setContainer("server");
        connection.openHandler(conn -> {
            log.log(Level.INFO, "Connection opened");
        }).closeHandler(conn -> {
            connection.close();
            connection.disconnect();
            log.log(Level.INFO, "Connection closed");
        }).disconnectHandler(protonConnection -> {
            connection.disconnect();
            log.log(Level.INFO, "Disconnected");
        }).open();

        connection.sessionOpenHandler(ProtonSession::open);
        connection.senderOpenHandler(this::senderOpenHandler);
    }

    private void senderOpenHandler(ProtonSender sender) {
        sender.setSource(sender.getRemoteSource());
        sender.open();
        database.subscribe(sender.getRemoteSource().getAddress(), new AMQPConfigSubscriber(sender));
    }

    public void run() {
        log.log(Level.INFO, String.format("Starting server on %s:%d", hostname, port));
        server.listen(port, hostname);
    }

    public int port() {
        return server.actualPort();
    }

    public void close() {
        server.close();
    }
}
