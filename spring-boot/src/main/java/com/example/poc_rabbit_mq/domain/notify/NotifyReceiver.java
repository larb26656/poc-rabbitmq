package com.example.poc_rabbit_mq.domain.notify;

import com.example.poc_rabbit_mq.entity.constants.NotifyState;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
@Slf4j
public class NotifyReceiver {

    private final int instance = 1;

    private final NotifyService notifyService;

    public NotifyReceiver(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    private void updateState(Long id) throws InterruptedException {
        // Mock delay...
        Thread.sleep(2000);

        notifyService.commit(id);
    }

    @RabbitListener(queues = "notify", containerFactory = "rabbitListenerContainerFactory")
    public void receive(String in, Channel channel, Message message) throws InterruptedException {
        StopWatch watch = new StopWatch();
        watch.start();

        Long id = Long.parseLong(in);

        try {
            updateState(id);

            watch.stop();
            log.info("instance " + this.instance + " [x] Done in " + watch.getTotalTimeSeconds() + "s");

            // Manual acknowledgment
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            watch.stop();

            log.error("instance " + this.instance + " [x] Error processing message", e);
//             Optionally, you can also reject the message and requeue or discard it
//             channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
