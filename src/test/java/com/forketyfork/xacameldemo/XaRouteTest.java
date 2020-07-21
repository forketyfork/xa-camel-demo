package com.forketyfork.xacameldemo;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
public class XaRouteTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    @Qualifier("nonXaJmsConnectionFactory")
    private ConnectionFactory jmsConnectionFactory;

    private JmsTemplate jmsTemplate;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setup() {
        jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(jmsConnectionFactory);

        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("delete from message");
    }

    @Test
    public void testFailure() {

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .fromRoute("queueRoute")
                .whenFailed(3)
                .create();

        jmsTemplate.send("TestQueue", session -> session.createTextMessage("testtesttest"));

        assertThat(notify.matches(10, TimeUnit.SECONDS)).isTrue();

        List<String> messages = jdbcTemplate.queryForList("select contents from message", String.class);

        assertThat(messages).isEmpty();
    }

    // this test fails, since the message is not yet committed to the database in the XA transaction
    // when NotifyBuilder reports route completion
    @Test
    @Transactional
    @Ignore
    public void testSuccessWrong() {

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .fromRoute("queueRoute")
                .whenCompleted(1)
                .create();

        jmsTemplate.send("TestQueue", session -> session.createTextMessage("testtestte"));

        assertThat(notify.matches(10, TimeUnit.SECONDS)).isTrue();

        List<String> messages = jdbcTemplate.queryForList("select contents from message", String.class);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isEqualTo("testtestte");

    }

    @Test
    @Transactional // you can also omit the @Transactional annotation here
    public void testSuccessRight() throws Exception {

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .fromRoute("queueRoute")
                .whenCompleted(1)
                .create();

        // setting the latch to 1
        CountDownLatch transactionLatch = new CountDownLatch(1);

        // adding a node as the first node of the route
        AdviceWithRouteBuilder.adviceWith(camelContext, "queueRoute", context ->
                context.weaveAddFirst().process(exchange ->
                        // adding transaction synchronization
                        TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronizationAdapter() {
                                    @Override
                                    public void afterCommit() {
                                        // when the transaction is finished, count down the latch
                                        transactionLatch.countDown();
                                    }
                                }))

        );

        // sending the message to the queue
        jmsTemplate.send("TestQueue", session -> session.createTextMessage("testtestte"));

        assertThat(notify.matches(20, TimeUnit.SECONDS)).isTrue();
        assertTrue(transactionLatch.await(5, TimeUnit.SECONDS));

        List<String> messages = jdbcTemplate.queryForList("select contents from message", String.class);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isEqualTo("testtestte");

    }

}
