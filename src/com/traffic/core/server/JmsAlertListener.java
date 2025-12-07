package com.traffic.core.server;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class JmsAlertListener implements MessageListener {

    public void startListening() {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnectionFactory.DEFAULT_BROKER_URL);
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("TRAFFIC_ALERTS_QUEUE");
            MessageConsumer consumer = session.createConsumer(destination);

            consumer.setMessageListener(this);
            System.out.println(">>> [JMS LISTENER] Police Station Listening for Alerts...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                System.out.println("!!! [POLICE STATION RECEIVED] " + textMessage.getText());
                // Here we could save to a different 'fines' database
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}