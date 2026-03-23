package com.fulfillment.shippingservice.application;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.fulfillment.shippingservice.domain.exception.WarehouseAccessDeniedException;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.WarehouseAccess;
import com.fulfillment.shippingservice.domain.ports.WarehouseAccessRepository;

@Service
public class ShippingWarehouseAuthorizationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_WAREHOUSE_MANAGER = "ROLE_WAREHOUSE_MANAGER";

    private final WarehouseAccessRepository warehouseAccessRepository;

    public ShippingWarehouseAuthorizationService(WarehouseAccessRepository warehouseAccessRepository) {
        this.warehouseAccessRepository = warehouseAccessRepository;
    }

    public void assertCanAccessShipment(Authentication authentication, Shipment shipment) {
        if (authentication == null) {
            return;
        }

        if (hasAuthority(authentication, ROLE_ADMIN)) {
            return;
        }

        String userId = authentication.getName();
        if (!hasAuthority(authentication, ROLE_WAREHOUSE_MANAGER)) {
            throw new WarehouseAccessDeniedException(userId, shipment.getWarehouseId());
        }

        boolean allowed = warehouseAccessRepository.findByUserId(userId)
            .filter(WarehouseAccess::isActive)
            .map(WarehouseAccess::getWarehouseId)
            .filter(shipment.getWarehouseId()::equals)
            .isPresent();

        if (!allowed) {
            throw new WarehouseAccessDeniedException(userId, shipment.getWarehouseId());
        }
    }

    public List<Shipment> filterAuthorizedShipments(Authentication authentication, List<Shipment> shipments) {
        if (authentication == null || hasAuthority(authentication, ROLE_ADMIN)) {
            return shipments;
        }

        String userId = authentication.getName();
        if (!hasAuthority(authentication, ROLE_WAREHOUSE_MANAGER)) {
            throw new WarehouseAccessDeniedException(userId, "unknown");
        }

        String assignedWarehouseId = warehouseAccessRepository.findByUserId(userId)
            .filter(WarehouseAccess::isActive)
            .map(WarehouseAccess::getWarehouseId)
            .orElseThrow(() -> new WarehouseAccessDeniedException(userId, "unknown"));

        return shipments.stream()
            .filter(shipment -> assignedWarehouseId.equals(shipment.getWarehouseId()))
            .toList();
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        Set<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
        return authorities.contains(authority);
    }
}
