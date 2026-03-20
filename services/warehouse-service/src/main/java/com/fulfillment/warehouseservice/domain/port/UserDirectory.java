package com.fulfillment.warehouseservice.domain.port;

import java.util.Optional;
import java.util.Set;

public interface UserDirectory {

    Optional<DirectoryUser> findById(String userId);

    record DirectoryUser(String userId, String username, Set<String> roles) {}
}
