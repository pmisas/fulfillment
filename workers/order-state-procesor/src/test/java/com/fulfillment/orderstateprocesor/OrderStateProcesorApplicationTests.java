package com.fulfillment.orderstateprocesor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@SpringBootTest
@ActiveProfiles("local")
class OrderStateProcesorApplicationTests {

	@MockitoBean
	OrderRepository orderRepository;

	@MockitoBean
	OrderStateHistoryRepository orderStateHistoryRepository;

	@MockitoBean
	SqsAsyncClient sqsAsyncClient;

	@Test
	void contextLoads() {
	}
}