package com.fulfillment.notificationlambda.domain.ports;

import java.util.Optional;

public interface OperatorEmailLookup {
    Optional<String> findEmailByOperatorId(String operatorId);
}
