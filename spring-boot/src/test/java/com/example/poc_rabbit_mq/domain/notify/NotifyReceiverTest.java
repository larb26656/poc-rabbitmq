package com.example.poc_rabbit_mq.domain.notify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.PublisherCallbackChannel;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@SpringRabbitTest()
@SpringBootTest()
class NotifyReceiverTest {

//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//
//    @Autowired
//    @Qualifier("notifyQueue")
//    private Queue queue;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    public void testSendMessage() {
//        String message = "Hello, RabbitMQ!";
//
//        rabbitTemplate.convertAndSend(this.queue.getName(), message);

//        verify(rabbitTemplate).convertAndSend(Mockito.eq("myQueue"), messageCaptor.capture());
//        assertEquals(message, messageCaptor.getValue());
    }

//    @Test
//    public void testTwoWay() throws Exception {
//        this.rabbitTemplate.convertAndSend(this.queue.getName(), "1");
//
//        PublisherCallbackChannel.Listener listener = this.harness.getSpy("1");
//        assertNotNull(listener);
//
//        System.out.println("aa");
//    }
}