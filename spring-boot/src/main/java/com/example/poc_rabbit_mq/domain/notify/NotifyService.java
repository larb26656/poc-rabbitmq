package com.example.poc_rabbit_mq.domain.notify;

import com.example.poc_rabbit_mq.entity.Notify;
import com.example.poc_rabbit_mq.entity.constants.NotifyState;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotifyService {

    private final NotifyRepository notifyRepository;

    public NotifyService(NotifyRepository notifyRepository) {
        this.notifyRepository = notifyRepository;
    }

    @Transactional
    Notify create() {
        val notify = new Notify();

        notify.setState(NotifyState.PENDING);

        notifyRepository.save(notify);

        return notify;
    }

    void commit(Long id) {
        val notify = notifyRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Notify not found"));

        notify.setState(NotifyState.SUCCESS);

        notifyRepository.save(notify);
    }
}
