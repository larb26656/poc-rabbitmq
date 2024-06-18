package com.example.poc_rabbit_mq.domain.notify;

import com.example.poc_rabbit_mq.constants.QueueName;
import com.example.poc_rabbit_mq.domain.notify.model.CommitNotifyReq;
import com.example.poc_rabbit_mq.entity.Notify;
import com.example.poc_rabbit_mq.entity.constants.NotifyState;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Testcontainers
class NotifyItTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Autowired
    private NotifyRepository notifyRepository;

    @Autowired
    RestClient.Builder builder;

    @SpyBean
    private NotifyService notifyService;

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:3.7.25");

    private String notifyApi;

    private String notifyCommitApi;

    @BeforeEach()
    void setup() throws Exception {
        String host = "http://localhost";
        notifyApi = host + ":" + port + "/api/notify";
        notifyCommitApi =  host + ":" + port + "/api/notify/commit";
    }

    private Notify createNotify(RestClient client) {
        return client
                .post()
                .uri(notifyApi)
                .retrieve()
                .body(Notify.class);
    }

    private void commitNotify(RestClient client, Long notifyId) {
        client
                .put()
                .uri(notifyCommitApi)
                .body(new CommitNotifyReq(
                        notifyId
                ))
                .retrieve()
                .toBodilessEntity();;
    }

    @Test
    void TestCommit_ShouldSuccess_WhenRabbitInvokeQueue() throws URISyntaxException {
        RestClient client = builder.build();

        Notify notify = createNotify(client);
        Long notifyId = notify.getId();

        commitNotify(client, notifyId);

        await().atMost(5, SECONDS).pollDelay(Duration.ofSeconds(1)).until(() -> {
            Optional<Notify> notifyOptional = notifyRepository.findById(notifyId);
            return notifyOptional.isPresent() && notifyOptional.get().getState().equals(NotifyState.SUCCESS);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void TestCommit_ShouldFailAndRetry_WhenRabbitInvokeQueue(int failRound) throws URISyntaxException, InterruptedException {
        // Arrange
        AtomicInteger count = new AtomicInteger(1);
        doAnswer(invocation -> {
            if (count.getAndIncrement() <= failRound) {
                System.out.println("Throwing error");
                throw new RuntimeException("Error occurred");
            } else {
                System.out.println("Calling real method");
                return invocation.callRealMethod();
            }
        }).when(notifyService).commit(any());

        // Act
        RestClient client = builder.build();

        Notify notify = createNotify(client);
        Long notifyId = notify.getId();

        commitNotify(client, notifyId);

        // Assert
        await().atMost(failRound + 2, SECONDS).pollDelay(Duration.ofSeconds(1)).until(() -> {
            Optional<Notify> notifyOptional = notifyRepository.findById(notifyId);
            return notifyOptional.isPresent() && notifyOptional.get().getState().equals(NotifyState.SUCCESS);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 9, 10})
    void TestCommit_ShouldFailRetryMaxThenSendToDLXWithReject_WhenRabbitInvokeQueue(int failRound) throws URISyntaxException, InterruptedException {
        // Arrange
        AtomicInteger count = new AtomicInteger(1);
        doAnswer(invocation -> {
            if (count.getAndIncrement() <= failRound) {
                System.out.println("Throwing error");
                throw new RuntimeException("Error occurred");
            } else {
                System.out.println("Calling real method");
                return invocation.callRealMethod();
            }
        }).when(notifyService).commit(any());

        // Act
        RestClient client = builder.build();

        Notify notify = createNotify(client);
        Long notifyId = notify.getId();

        commitNotify(client, notifyId);


        // Assert
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Message message = rabbitTemplate.receive(QueueName.NOTIFY_DLX, 5000);
                assertNotNull(message);
                String firstDeathQueue = message.getMessageProperties().getHeader("x-first-death-queue");
                String firstDeathReason = message.getMessageProperties().getHeader("x-first-death-reason");

                assertEquals("notify", firstDeathQueue);
                assertEquals("rejected", firstDeathReason);
            });
    }
}