package com.fulfillment.warehouseservice.infrastructure.identity;

import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.warehouseservice.domain.port.UserDirectory;

@Component
@Profile("local")
public class LocalUserDirectory implements UserDirectory {

    @Override
    public Optional<DirectoryUser> findById(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new DirectoryUser(userId.trim(), userId.trim(), Set.of("WAREHOUSE_MANAGER")));
    }
}
