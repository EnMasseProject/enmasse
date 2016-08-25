package enmasse.perf

import javax.jms.*
import javax.naming.Context

/**
 * @author Ulf Lilleengen
 */
class TestSender(val context: Context) {

    fun sendMessages(numMessages: Int, address: String): Int {
        val connectionFactory = context.lookup("enmasse") as ConnectionFactory
        val destination = context.lookup(address) as Destination

        val connection = connectionFactory.createConnection()
        connection.start();

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

        val messageProducer = session.createProducer(destination)

        var messagesSent = 0
        for (i in 1.rangeTo(numMessages)) {
            val message = session.createTextMessage("Message ${i}")
            message.jmsCorrelationID = "${i}"
            messageProducer.send(message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE)
            messagesSent++
        }

        messageProducer.close()
        session.close()
        connection.close()
        return messagesSent
    }
}
