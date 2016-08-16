package enmasse.broker.prestop

import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.proton.ProtonClient
import io.vertx.proton.ProtonQoS
import org.apache.qpid.proton.message.Message
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Ulf Lilleengen
 */
class Client(val address: String, val debugFn: Optional<() -> Unit>) {

    fun drainMessages(from: Endpoint, to: Endpoint) {
        val vertx = Vertx.vertx()
        val fromClient = ProtonClient.create(vertx)
        val toClient = ProtonClient.create(vertx);

        val first =  AtomicBoolean(false)
        toClient.connect(to.hostName, to.port, { handle ->
            if (handle.succeeded()) {
                val sender = handle.result().open().createSender(address)
                        .setQoS(ProtonQoS.AT_LEAST_ONCE).open()
                fromClient.connect(from.hostName, from.port, { handle ->
                    if (handle.succeeded()) {
                        val receiver = handle.result().open().createReceiver(address).setQoS(ProtonQoS.AT_LEAST_ONCE).open()
                        receiver.handler { protonDelivery, message ->

                            val forwardedMessage = Message.Factory.create()

                            forwardedMessage.address = message.address
                            forwardedMessage.body = message.body

                            sender.send(forwardedMessage)

                            // This is for debugging only
                            if (!first.getAndSet(true) && debugFn.isPresent) {
                                vertx.executeBlocking<Int>({ future -> debugFn.get().invoke(); future.complete(0)}, { handler:AsyncResult<Int> -> 0})
                            }
                        }
                    }
                })
            }
        })
    }


}


