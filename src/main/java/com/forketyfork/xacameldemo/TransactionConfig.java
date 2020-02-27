package com.forketyfork.xacameldemo;

import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class TransactionConfig {

    @Bean("policyPropagationRequired")
    public SpringTransactionPolicy transactionPolicyPropagationRequired(
            @Autowired JtaTransactionManager transactionManager) {
        SpringTransactionPolicy policy = new SpringTransactionPolicy(transactionManager);
        policy.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return policy;
    }

}
