package com.fulfillment.orderservice.karate;

import com.intuit.karate.junit5.Karate;

class OrderContractKarateRunner {

    @Karate.Test
    Karate runContracts() {
        return Karate.run("classpath:features/contracts").relativeTo(getClass());
    }
}
