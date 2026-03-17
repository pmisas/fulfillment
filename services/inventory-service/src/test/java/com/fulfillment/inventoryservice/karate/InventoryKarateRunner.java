package com.fulfillment.inventoryservice.karate;

import com.intuit.karate.junit5.Karate;

class InventoryKarateRunner {

    @Karate.Test
    Karate runAll() {
        return Karate.run("classpath:features/inventory").relativeTo(getClass());
    }
}
