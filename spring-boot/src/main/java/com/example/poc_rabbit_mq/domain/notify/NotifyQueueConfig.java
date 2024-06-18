package com.example.poc_rabbit_mq.domain.notify;

import com.example.poc_rabbit_mq.constants.QueueName;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class NotifyQueueConfig {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public RabbitAdmin rabbitAdmin()
    {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue notifyQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "");
        arguments.put("x-dead-letter-routing-key", QueueName.NOTIFY_DLX);
        return new Queue("notify", true, false, false, arguments);
    }

    @Bean
    public Queue notifyDlxQueue() {
        return new Queue(QueueName.NOTIFY_DLX, true);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(5);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        System.out.println("Prefetch count set to 5");
        return factory;
    }
}
