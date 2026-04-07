package com.fulfillment.inventoryservice.karate;

import com.intuit.karate.junit5.Karate;

class InventoryContractKarateRunner {

    @Karate.Test
    Karate runContracts() {
        return Karate.run("classpath:features/contracts").relativeTo(getClass());
    }
}
