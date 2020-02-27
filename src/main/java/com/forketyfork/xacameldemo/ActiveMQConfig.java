package com.forketyfork.xacameldemo;

import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.jms.ConnectionFactory;

@Configuration
public class ActiveMQConfig {

    @Bean("activemq")
    public ActiveMQComponent activeMq(
            ConnectionFactory connectionFactory,
            JtaTransactionManager jtaTransactionManager) {
        ActiveMQComponent component = new ActiveMQComponent();
        component.setAcknowledgementMode(JmsProperties.AcknowledgeMode.CLIENT.getMode());
        component.setCacheLevelName("CACHE_CONSUMER");
        component.setConnectionFactory(connectionFactory);
        component.setTransacted(true);
        component.setTransactionManager(jtaTransactionManager);
        return component;
    }

    @Bean
    public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer() {
        return factory -> {
            RedeliveryPolicy policy = new RedeliveryPolicy();
            policy.setRedeliveryDelay(1000);
            policy.setMaximumRedeliveries(2);
            factory.setRedeliveryPolicy(policy);
        };
    }

}
