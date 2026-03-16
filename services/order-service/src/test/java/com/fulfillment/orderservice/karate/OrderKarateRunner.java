package com.fulfillment.orderservice.karate;

import com.intuit.karate.junit5.Karate;

class OrderKarateRunner {

    @Karate.Test
    Karate runAll() {
        return Karate.run("classpath:features/order").relativeTo(getClass());
    }
}
