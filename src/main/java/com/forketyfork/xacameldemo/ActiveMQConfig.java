package com.forketyfork.xacameldemo;

import jakarta.jms.ConnectionFactory;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jms.connection.JmsTransactionManager;

@Configuration
public class ActiveMQConfig {

    @Bean
    public ActiveMQComponent activemq(
            ConnectionFactory connectionFactory) {
        ActiveMQComponent component = new ActiveMQComponent();
        component.setAcknowledgementMode(jakarta.jms.Session.CLIENT_ACKNOWLEDGE);
        component.setCacheLevelName("CACHE_CONSUMER");
        component.setConnectionFactory(connectionFactory);
        component.setTransacted(true);
        return component;
    }

    @Bean
    public PlatformTransactionManager jmsTransactionManager(ConnectionFactory connectionFactory) {
        return new JmsTransactionManager(connectionFactory);
    }
}
