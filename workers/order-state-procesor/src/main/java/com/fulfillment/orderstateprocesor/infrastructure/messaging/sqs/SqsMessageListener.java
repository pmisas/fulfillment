package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fulfillment.orderstateprocesor.application.OrderStateProcessorService;
import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

@Component
public class SqsMessageListener {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageListener.class);

    private final SqsAsyncClient sqsAsync;
    private final OrderStateProcessorService processor;
    private final SqsMessageMapper mapper;

    private final String queueUrl;
    private final int waitTimeSeconds;
    private final int maxMessages;
    private final int visibilityTimeoutSeconds;
    private final int maxConcurrentProcessing;

    private Disposable subscription;

    public SqsMessageListener(
        SqsAsyncClient sqsAsync,
        OrderStateProcessorService processor,
        SqsMessageMapper mapper,
        @Value("${aws.sqs.queueUrl}") String queueUrl,
        @Value("${aws.sqs.waitTimeSeconds:10}") int waitTimeSeconds,
        @Value("${aws.sqs.maxMessages:10}") int maxMessages,
        @Value("${aws.sqs.visibilityTimeoutSeconds:30}") int visibilityTimeoutSeconds,
        @Value("${worker.maxConcurrentProcessing:5}") int maxConcurrentProcessing
    ) {
        this.sqsAsync = sqsAsync;
        this.processor = processor;
        this.mapper = mapper;
        this.queueUrl = queueUrl;
        this.waitTimeSeconds = waitTimeSeconds;
        this.maxMessages = maxMessages;
        this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
        this.maxConcurrentProcessing = maxConcurrentProcessing;
    }

    @PostConstruct
    public void start() {
        subscription = pollLoop()
            .repeat()
            .retryWhen(Retry
                .backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(30))
                .doBeforeRetry(signal ->
                    log.warn("SQS poll error, retrying: {}", signal.failure().getMessage())))
            .subscribe();
        log.info("Reactive SQS listener started for queue: {}", queueUrl);
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("Reactive SQS listener stopped");
        }
    }

    private Mono<Void> pollLoop() {
        ReceiveMessageRequest req = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .waitTimeSeconds(waitTimeSeconds)
            .maxNumberOfMessages(maxMessages)
            .visibilityTimeout(visibilityTimeoutSeconds)
            .messageAttributeNames("All")
            .build();

        return Mono.fromFuture(() -> sqsAsync.receiveMessage(req))
            .flatMapMany(resp -> Flux.fromIterable(
                resp.messages() != null ? resp.messages() : List.of()))
            .flatMap(this::processAndDelete, maxConcurrentProcessing)
            .then();
    }

    private Mono<Void> processAndDelete(Message msg) {
        return Mono.defer(() -> {
                ProcessEventCommand cmd = mapper.toCommand(msg);
                return processor.process(cmd);
            })
            .then(deleteMessage(msg))
            .onErrorResume(ex -> {
                log.error("Failed processing messageId={}: {}", msg.messageId(), ex.getMessage(), ex);
                return Mono.empty();
            });
    }

    private Mono<Void> deleteMessage(Message msg) {
        DeleteMessageRequest del = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(msg.receiptHandle())
            .build();
        return Mono.fromFuture(() -> sqsAsync.deleteMessage(del)).then();
    }
}
