package com.fulfillment.warehouseservice.application;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fulfillment.warehouseservice.application.dto.AssignWarehouseManagerCommand;
import com.fulfillment.warehouseservice.domain.exception.UserNotFoundException;
import com.fulfillment.warehouseservice.domain.exception.UserRoleNotAllowedException;
import com.fulfillment.warehouseservice.domain.exception.WarehouseAccessNotFoundException;
import com.fulfillment.warehouseservice.domain.exception.WarehouseManagerAssignmentConflictException;
import com.fulfillment.warehouseservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;
import com.fulfillment.warehouseservice.domain.port.UserDirectory;
import com.fulfillment.warehouseservice.domain.port.WarehouseAccessRepository;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;

import static com.fulfillment.warehouseservice.domain.shared.DomainValidations.requireNonBlank;

@Service
public class WarehouseAccessServiceImpl implements WarehouseAccessService {

    private static final String WAREHOUSE_MANAGER_ROLE = "WAREHOUSE_MANAGER";

    private final WarehouseAccessRepository warehouseAccessRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserDirectory userDirectory;

    public WarehouseAccessServiceImpl(
            WarehouseAccessRepository warehouseAccessRepository,
            WarehouseRepository warehouseRepository,
            UserDirectory userDirectory) {
        this.warehouseAccessRepository = warehouseAccessRepository;
        this.warehouseRepository = warehouseRepository;
        this.userDirectory = userDirectory;
    }

    @Override
    public WarehouseAccess assignManager(AssignWarehouseManagerCommand command) {
        String warehouseId = requireNonBlank(command.warehouseId(), "warehouseId").trim();
        String userId = requireNonBlank(command.userId(), "userId").trim();

        ensureWarehouseExists(warehouseId);
        ensureWarehouseManagerUser(userId);

        warehouseAccessRepository.findByUserId(userId)
            .filter(WarehouseAccess::isActive)
            .filter(access -> access.getWarehouseId().equals(warehouseId))
            .ifPresent(access -> {
                throw new WarehouseManagerAssignmentConflictException(userId, warehouseId);
            });

        WarehouseAccess assignment = WarehouseAccess.assign(userId, warehouseId, command.assignedBy(), Instant.now());
        return warehouseAccessRepository.save(assignment);
    }

    @Override
    public WarehouseAccess removeManager(String warehouseId, String userId) {
        String normalizedWarehouseId = requireNonBlank(warehouseId, "warehouseId").trim();
        String normalizedUserId = requireNonBlank(userId, "userId").trim();

        ensureWarehouseExists(normalizedWarehouseId);

        WarehouseAccess access = warehouseAccessRepository.findByUserId(normalizedUserId)
            .filter(WarehouseAccess::isActive)
            .filter(assignment -> assignment.getWarehouseId().equals(normalizedWarehouseId))
            .orElseThrow(() -> new WarehouseAccessNotFoundException(normalizedUserId));

        return warehouseAccessRepository.save(access.deactivate(Instant.now()));
    }

    @Override
    public List<String> getManagersByWarehouse(String warehouseId) {
        String normalizedWarehouseId = requireNonBlank(warehouseId, "warehouseId").trim();
        ensureWarehouseExists(normalizedWarehouseId);

        return warehouseAccessRepository.findActiveByWarehouseId(normalizedWarehouseId).stream()
            .map(WarehouseAccess::getUserId)
            .toList();
    }

    @Override
    public WarehouseAccess getWarehouseAccessByUser(String userId) {
        String normalizedUserId = requireNonBlank(userId, "userId").trim();

        return warehouseAccessRepository.findByUserId(normalizedUserId)
            .orElseThrow(() -> new WarehouseAccessNotFoundException(normalizedUserId));
    }

    private void ensureWarehouseExists(String warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(warehouseId);
        }
    }

    private void ensureWarehouseManagerUser(String userId) {
        UserDirectory.DirectoryUser user = userDirectory.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        Set<String> roles = user.roles() == null ? Set.of() : user.roles();
        if (!roles.contains(WAREHOUSE_MANAGER_ROLE)) {
            throw new UserRoleNotAllowedException(userId, WAREHOUSE_MANAGER_ROLE);
        }
    }
}
