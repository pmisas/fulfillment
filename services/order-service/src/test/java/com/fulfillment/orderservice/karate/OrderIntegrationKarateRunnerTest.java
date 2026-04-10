package com.fulfillment.orderservice.karate;

import com.intuit.karate.junit5.Karate;

class OrderIntegrationKarateRunnerTest {

    @Karate.Test
    Karate runIntegration() {
        return Karate.run("classpath:features/integration");
    }
}
