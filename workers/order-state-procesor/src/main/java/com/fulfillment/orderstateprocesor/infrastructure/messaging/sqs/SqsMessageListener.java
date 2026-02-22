package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fulfillment.orderstateprocesor.application.OrderStateProcessorService;
import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@Component
@EnableScheduling
public class SqsMessageListener {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageListener.class);

    private final SqsClient sqs;
    private final OrderStateProcessorService processor;
    private final SqsMessageMapper mapper;

    private final String queueUrl;
    private final int waitTimeSeconds;
    private final int maxMessages;
    private final int visibilityTimeoutSeconds;

    public SqsMessageListener(
        SqsClient sqs,
        OrderStateProcessorService processor,
        SqsMessageMapper mapper,
        @Value("${aws.sqs.queueUrl}") String queueUrl,
        @Value("${aws.sqs.waitTimeSeconds:10}") int waitTimeSeconds,
        @Value("${aws.sqs.maxMessages:10}") int maxMessages,
        @Value("${aws.sqs.visibilityTimeoutSeconds:30}") int visibilityTimeoutSeconds
    ) {
        this.sqs = sqs;
        this.processor = processor;
        this.mapper = mapper;
        this.queueUrl = queueUrl;
        this.waitTimeSeconds = waitTimeSeconds;
        this.maxMessages = maxMessages;
        this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${worker.poll.fixedDelayMs:1000}")
    public void poll() {
        try {
            ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(waitTimeSeconds)
                .maxNumberOfMessages(maxMessages)
                .visibilityTimeout(visibilityTimeoutSeconds)
                .messageAttributeNames("All")
                .build();

            ReceiveMessageResponse resp = sqs.receiveMessage(req);
            List<Message> messages = resp.messages();
            if (messages == null || messages.isEmpty()) return;

            for (Message m : messages) {
                boolean ok = handleOne(m);
                if (ok) {
                    delete(m);
                } else {
                }
            }
        } catch (Exception e) {
            log.error("SQS poll error", e);
        }
    }

    private boolean handleOne(Message msg) {
        try {
            ProcessEventCommand cmd = mapper.toCommand(msg);
            processor.process(cmd);
            return true;
        } catch (Exception ex) {
            log.error("Failed processing messageId={} err={}", msg.messageId(), ex.getMessage(), ex);
            return false;
        }
    }

    private void delete(Message msg) {
        DeleteMessageRequest del = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(msg.receiptHandle())
            .build();
        sqs.deleteMessage(del);
    }
}