package com.fulfillment.inventoryservice.application;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.fulfillment.inventoryservice.domain.exception.WarehouseAccessDeniedException;
import com.fulfillment.inventoryservice.domain.model.WarehouseAccess;
import com.fulfillment.inventoryservice.domain.ports.WarehouseAccessRepository;

@Service
public class InventoryWarehouseAuthorizationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_WAREHOUSE_MANAGER = "ROLE_WAREHOUSE_MANAGER";

    private final WarehouseAccessRepository warehouseAccessRepository;

    public InventoryWarehouseAuthorizationService(WarehouseAccessRepository warehouseAccessRepository) {
        this.warehouseAccessRepository = warehouseAccessRepository;
    }

    public void assertCanAccessWarehouse(Authentication authentication, String warehouseId) {
        if (authentication == null) {
            return;
        }

        Set<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        if (authorities.contains(ROLE_ADMIN)) {
            return;
        }

        String userId = authentication.getName();
        if (!authorities.contains(ROLE_WAREHOUSE_MANAGER)) {
            throw new WarehouseAccessDeniedException(userId, warehouseId);
        }

        boolean allowed = warehouseAccessRepository.findByUserId(userId)
            .filter(WarehouseAccess::isActive)
            .map(WarehouseAccess::getWarehouseId)
            .filter(warehouseId::equals)
            .isPresent();

        if (!allowed) {
            throw new WarehouseAccessDeniedException(userId, warehouseId);
        }
    }
}
