package com.fulfillment.orderstateprocesor.domain.ports;

import java.util.List;

public interface WarehouseClient {
    boolean existsById(String warehouseId);
    List<String> listWarehouseIds(); 
}