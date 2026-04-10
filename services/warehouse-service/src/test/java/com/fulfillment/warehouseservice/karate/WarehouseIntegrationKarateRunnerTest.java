package com.fulfillment.warehouseservice.karate;

import com.intuit.karate.junit5.Karate;

class WarehouseIntegrationKarateRunner {

    @Karate.Test
    Karate runIntegration() {
        return Karate.run("classpath:features/integration");
    }
}
