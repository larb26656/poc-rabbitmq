package com.example.poc_rabbit_mq.domain.notify;

import com.example.poc_rabbit_mq.domain.notify.model.CommitNotifyReq;
import com.example.poc_rabbit_mq.entity.Notify;
import jakarta.validation.Valid;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/notify")
public class NotifyController {

    private final RabbitTemplate template;

    private final Queue queue;

    private final NotifyService notifyService;

    public NotifyController(RabbitTemplate template, @Qualifier("notifyQueue") Queue queue, NotifyService notifyService) {
        this.template = template;
        this.queue = queue;
        this.notifyService = notifyService;
    }

    @PostMapping
    public Notify save() {
        return notifyService.create();
    }

    @PutMapping("/commit")
    public void commit(@Valid @RequestBody CommitNotifyReq req) {
        this.template.convertAndSend(queue.getName(), req.id());
        System.out.println(" [x] Sent commit'" + req.id() + "'");
    }
}
