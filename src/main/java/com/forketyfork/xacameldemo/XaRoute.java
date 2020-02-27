package com.forketyfork.xacameldemo;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class XaRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("activemq:TestQueue")
                .transacted("policyPropagationRequired")
                .to("sql:insert into message(contents) values(:#${body})")
                .log("Delay...")
                .delay(10000)
                .log("Delay finished, committing");
    }
}
