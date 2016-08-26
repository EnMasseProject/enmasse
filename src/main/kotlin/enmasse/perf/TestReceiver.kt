package enmasse.perf

import java.util.concurrent.TimeUnit
import javax.jms.ConnectionFactory
import javax.jms.Destination
import javax.jms.Session
import javax.naming.Context

class TestReceiver(val context: Context, val address: String) {

    fun recvMessages(numMessages: Int): List<String> {
        val connectionFactory = context.lookup("enmasse") as ConnectionFactory
        val destination = context.lookup(address) as Destination

        val connection = connectWithTimeout(connectionFactory, 60, TimeUnit.SECONDS)
        connection.start();

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

        val consumer = session.createConsumer(destination)

        var numReceived = 0
        val receivedMessages = 1.rangeTo(numMessages).map { i ->
            val message = consumer.receive()
            numReceived++
            message.toString()
        }

        consumer.close()
        session.close()
        connection.close()
        return receivedMessages
    }
}