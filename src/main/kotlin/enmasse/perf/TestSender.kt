package enmasse.perf

import java.util.concurrent.TimeUnit
import javax.jms.*
import javax.naming.Context

/**
 * @author Ulf Lilleengen
 */
class TestSender(val context: Context, val address: String) {

    fun sendMessages(messages: List<String>, connectTimeout: Long, timeUnit: TimeUnit): Int {
        val connectionFactory = context.lookup("enmasse") as ConnectionFactory
        val destination = context.lookup(address) as Destination

        val connection = connectWithTimeout(connectionFactory, connectTimeout, timeUnit)
        connection.start();

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

        val messageProducer = session.createProducer(destination)

        var messagesSent = 0
        for (msg in messages) {
            val message = session.createTextMessage(msg)
            message.jmsCorrelationID = "${++messagesSent}"
            messageProducer.send(message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE)
        }

        messageProducer.close()
        session.close()
        connection.close()
        return messagesSent
    }
}
