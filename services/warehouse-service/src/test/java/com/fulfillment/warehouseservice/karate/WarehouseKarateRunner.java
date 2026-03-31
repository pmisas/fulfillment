package com.fulfillment.warehouseservice.karate;

import com.intuit.karate.junit5.Karate;

class WarehouseKarateRunner {

    @Karate.Test
    Karate runAll() {
        return Karate.run("classpath:features/integration");
    }
}
