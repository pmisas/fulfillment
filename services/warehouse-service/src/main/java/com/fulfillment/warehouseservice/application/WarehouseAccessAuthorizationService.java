package com.fulfillment.warehouseservice.application;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.fulfillment.warehouseservice.domain.exception.WarehouseAccessDeniedException;
import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;
import com.fulfillment.warehouseservice.domain.port.WarehouseAccessRepository;

import static com.fulfillment.warehouseservice.domain.shared.DomainValidations.requireNonBlank;

@Service
public class WarehouseAccessAuthorizationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_WAREHOUSE_MANAGER = "ROLE_WAREHOUSE_MANAGER";

    private final WarehouseAccessRepository warehouseAccessRepository;

    public WarehouseAccessAuthorizationService(WarehouseAccessRepository warehouseAccessRepository) {
        this.warehouseAccessRepository = warehouseAccessRepository;
    }

    public void assertCanAccessWarehouse(Authentication authentication, String warehouseId) {
        if (authentication == null) {
            return;
        }

        String normalizedWarehouseId = requireNonBlank(warehouseId, "warehouseId").trim();
        Set<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        if (authorities.contains(ROLE_ADMIN)) {
            return;
        }

        String userId = authentication.getName();
        if (!authorities.contains(ROLE_WAREHOUSE_MANAGER)) {
            throw new WarehouseAccessDeniedException(userId, normalizedWarehouseId);
        }

        boolean allowed = warehouseAccessRepository.findByUserId(userId)
            .filter(WarehouseAccess::isActive)
            .map(WarehouseAccess::getWarehouseId)
            .filter(normalizedWarehouseId::equals)
            .isPresent();

        if (!allowed) {
            throw new WarehouseAccessDeniedException(userId, normalizedWarehouseId);
        }
    }
}
