package enmasse.perf

import java.util.concurrent.TimeUnit
import javax.jms.*
import javax.naming.Context

/**
 * @author Ulf Lilleengen
 */
class TestSender(val context: Context) {

    fun sendMessages(numMessages: Int, address: String): Int {
        val connectionFactory = context.lookup("enmasse") as ConnectionFactory
        println("Looked up connection factory")
        val destination = context.lookup(address) as Destination
        println("Looked up destination")

        val connection = connectWithTimeout(connectionFactory, 60, TimeUnit.SECONDS)
        println("Created connection")
        connection.start();
        println("Started connection")

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
