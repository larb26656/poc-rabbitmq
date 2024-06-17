package com.example.poc_rabbit_mq.domain.notify;

import com.example.poc_rabbit_mq.entity.Notify;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifyRepository extends JpaRepository<Notify, Long> {
}
