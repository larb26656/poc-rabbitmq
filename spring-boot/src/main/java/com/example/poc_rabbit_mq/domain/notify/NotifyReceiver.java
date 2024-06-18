package com.example.poc_rabbit_mq.domain.notify;

import com.example.poc_rabbit_mq.constants.MessageHeader;
import com.example.poc_rabbit_mq.constants.QueueName;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class NotifyReceiver {

    private final static int MAX_RETRIES_COUNT = 5;

    private final NotifyService notifyService;

    private final RabbitTemplate template;

    private final Queue queue;

    public NotifyReceiver(NotifyService notifyService, RabbitTemplate template, @Qualifier("notifyQueue") Queue queue) {
        this.notifyService = notifyService;
        this.template = template;
        this.queue = queue;
    }

    public void updateState(Long id) throws InterruptedException {
        notifyService.commit(id);
    }

    @RabbitListener(id = "receive", queues = QueueName.NOTIFY, containerFactory = "rabbitListenerContainerFactory")
    public void receive(String in, Channel channel, Message message) throws InterruptedException, IOException {
        StopWatch watch = new StopWatch();
        watch.start();

        Long id = Long.parseLong(in);
        Map<String, Object> headerMap = message.getMessageProperties().getHeaders();
        int retryCount = (int) headerMap.getOrDefault(MessageHeader.RETRY_COUNT, 0);

        log.info("[x] Received message with retry count: {}", retryCount);

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            updateState(id);
            watch.stop();
            log.info("[x] Processing done in {}s", watch.getTotalTimeSeconds());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            handleProcessingError(e, retryCount, deliveryTag, id, channel);
        }
    }

    private void handleProcessingError(Exception e, int retryCount, long deliveryTag, Long id, Channel channel) throws IOException {
        if (retryCount >= MAX_RETRIES_COUNT) {
            log.error("[x] Reached max retry count!");
            channel.basicNack(deliveryTag, false, false);
        } else {
            log.error("[x] Error processing message", e);
            channel.basicAck(deliveryTag, false);
            resendMessageWithRetry(id, retryCount);
        }
    }

    private void resendMessageWithRetry(Long id, int retryCount) {
        template.convertAndSend(queue.getName(), id, message -> {
            message.getMessageProperties().getHeaders().put(MessageHeader.RETRY_COUNT, retryCount + 1);
            return message;
        });
    }
}
