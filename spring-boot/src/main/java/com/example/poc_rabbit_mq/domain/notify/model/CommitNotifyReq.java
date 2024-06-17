package com.example.poc_rabbit_mq.domain.notify.model;

import jakarta.validation.constraints.NotNull;

public record CommitNotifyReq(
        @NotNull
        Long id
) {
}
