package com.fulfillment.inventoryservice.karate;

import com.intuit.karate.junit5.Karate;

class InventoryIntegrationKarateRunner {

    @Karate.Test
    Karate runIntegration() {
        return Karate.run("classpath:features/integration");
    }
}
