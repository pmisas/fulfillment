package com.fulfillment.shippingservice.karate;

import com.intuit.karate.junit5.Karate;

class ShippingKarateRunner {

    @Karate.Test
    Karate runAll() {
        return Karate.run("classpath:features/shipping").relativeTo(getClass());
    }
}
