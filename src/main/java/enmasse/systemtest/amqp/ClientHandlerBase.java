package enmasse.systemtest.amqp;

import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.reactor.FlowController;
import org.apache.qpid.proton.reactor.Handshaker;

import java.util.concurrent.CountDownLatch;

public abstract class ClientHandlerBase extends BaseHandler {

    private final enmasse.systemtest.Endpoint endpoint;
    private final CountDownLatch connectLatch;
    protected final ClientOptions clientOptions;

    public ClientHandlerBase(enmasse.systemtest.Endpoint endpoint, ClientOptions clientOptions, CountDownLatch connectLatch) {
        add(new Handshaker());
        add(new FlowController());
        this.endpoint = endpoint;
        this.clientOptions = clientOptions;
        this.connectLatch = connectLatch;
    }


    @Override
    public void onReactorInit(Event event) {
        event.getReactor().connectionToHost(endpoint.getHost(), endpoint.getPort(), this);
    }

    @Override
    public void onConnectionInit(Event event) {
        Connection connection = event.getConnection();
        connection.setHostname("enmasse-systemtest-client");
        connection.open();
    }

    @Override
    public void onConnectionBound(Event event) {
        Connection connection = event.getConnection();
        if (clientOptions.getSslOptions().isPresent()) {
            connection.getTransport().ssl(clientOptions.getSslOptions().get().getSslDomain(), clientOptions.getSslOptions().get().getSslPeerDetails());
        }
    }

    @Override
    public void onConnectionRemoteOpen(Event event) {
        Connection connection = event.getConnection();
        Session session = connection.session();
        session.open();

        openLink(session);

    }

    protected abstract void openLink(Session session);

    @Override
    public void onConnectionFinal(Event event) {
        handleError(event.getConnection().getCondition());
    }

    @Override
    public void onSessionFinal(Event event) {
        handleError(event.getSession().getCondition());
    }

    @Override
    public void onLinkFinal(Event event) {
        handleError(event.getLink().getCondition());
    }

    @Override
    public void onTransportError(Event event) {
        handleError(event.getConnection().getCondition());
    }

    @Override
    public void onLinkRemoteOpen(Event event) {
        System.out.println("Client connected");
        connectLatch.countDown();
    }

    protected void handleError(ErrorCondition error) {
        if (error == null || error.getCondition() == null) {
            System.out.println("Link closed without error");
        } else {
            System.out.println("Link closed with " + error);
            reportException(new RuntimeException(error.getDescription()));
        }
    }

    protected abstract void reportException(Exception e);
}
