package com.example.poc_rabbit_mq.domain.notify;

import com.example.poc_rabbit_mq.domain.notify.model.CommitNotifyReq;
import com.example.poc_rabbit_mq.entity.Notify;
import com.example.poc_rabbit_mq.entity.constants.NotifyState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Testcontainers
class NotifyItTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotifyRepository notifyRepository;

    @Autowired
    RestClient.Builder builder;

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:3.7.25");

    @Test
    void TestCommit_ShouldSuccess_WhenRabbitInvokeQueue() throws URISyntaxException {
        RestClient client = builder.build();

        Notify createNotify = client
                .post()
                .uri(new URI("http://localhost:" + port + "/api/notify"))
                .retrieve()
                .body(Notify.class);

        Long notifyId = createNotify.getId();

        client
                .put()
                .uri(new URI("http://localhost:" + port + "/api/notify/commit"))
                .body(new CommitNotifyReq(
                        notifyId
                ))
                .retrieve()
                .toBodilessEntity();

        await().atMost(5, SECONDS).pollDelay(Duration.ofSeconds(1)).until(() -> {
            Optional<Notify> notifyOptional = notifyRepository.findById(notifyId);
            return notifyOptional.isPresent() && notifyOptional.get().getState().equals(NotifyState.SUCCESS);
        });
    }

}