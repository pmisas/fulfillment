package com.fulfillment.orderstateprocesor.domain.ports;

import java.util.List;

public interface WarehouseClient {

    boolean existsById(String warehouseId);
    List<WarehouseSummary> listWarehouses();

    record WarehouseSummary(String warehouseId, double lat, double lng) {}
}
