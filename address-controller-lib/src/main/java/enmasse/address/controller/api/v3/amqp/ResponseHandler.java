package enmasse.address.controller.api.v3.amqp;

import org.apache.qpid.proton.message.Message;

/**
 * Handles the response.
 */
public interface ResponseHandler {
    void sendResponse(Message message);
}
