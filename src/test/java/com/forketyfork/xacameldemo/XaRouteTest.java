package com.forketyfork.xacameldemo;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import org.apache.camel.CamelContext;
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
                .from("activemq:TestQueue")
                .whenFailed(3)
                .create();

        sendMessage("testtesttest", "TestQueue");

        assertThat(notify.matches(10, TimeUnit.SECONDS)).isTrue();

        List<String> messages = jdbcTemplate.queryForList("select contents from message", String.class);

        assertThat(messages).isEmpty();
    }

    public void sendMessage(String message, String queueName) {
        jmsTemplate.send(queueName, session -> session.createTextMessage(message));
    }

    // this test it fails, since the message is not yet committed to the database in the XA transaction
    // when NotifyBuilder reports route completion
    @Test
    @Transactional
    @Ignore
    public void testSuccessWrong() {



        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .from("activemq:TestQueue")
                .whenCompleted(1)
                .create();

        sendMessage("testtestte", "TestQueue");

        assertThat(notify.matches(20, TimeUnit.SECONDS)).isTrue();

        List<String> messages = jdbcTemplate.queryForList("select contents from message", String.class);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isEqualTo("testtestte");

    }

    @Test
    @Transactional // you can also remove the @Transactional annotation here
    public void testSuccessRight() throws Exception {

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .from("activemq:TestQueue")
                .whenCompleted(1)
                .create();

        CountDownLatch transactionLatch = new CountDownLatch(1);
        AdviceWithRouteBuilder.adviceWith(camelContext, "queueRoute", context ->
                context.weaveAddFirst().process(exchange ->
                        TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronizationAdapter() {
                                    @Override
                                    public void afterCommit() {
                                        transactionLatch.countDown();
                                    }
                                }))

        );
        sendMessage("testtestte", "TestQueue");

        assertThat(notify.matches(20, TimeUnit.SECONDS)).isTrue();
        assertTrue(transactionLatch.await(5, TimeUnit.SECONDS));

        List<String> messages = jdbcTemplate.queryForList("select contents from message", String.class);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isEqualTo("testtestte");

    }

}
