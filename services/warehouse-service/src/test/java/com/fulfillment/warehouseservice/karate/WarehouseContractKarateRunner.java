package com.fulfillment.warehouseservice.karate;

import com.intuit.karate.junit5.Karate;

class WarehouseContractKarateRunner {

    @Karate.Test
    Karate runContracts() {
        return Karate.run("classpath:features/contracts");
    }
}
