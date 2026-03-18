package com.fulfillment.notificationlambda.infrastructure.operator;

import com.fulfillment.notificationlambda.domain.ports.OperatorEmailLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.util.Optional;

public class CognitoOperatorEmailLookup implements OperatorEmailLookup {

    private static final Logger log = LoggerFactory.getLogger(CognitoOperatorEmailLookup.class);

    private final CognitoIdentityProviderClient cognito;
    private final String userPoolId;

    public CognitoOperatorEmailLookup(CognitoIdentityProviderClient cognito, String userPoolId) {
        this.cognito = cognito;
        this.userPoolId = userPoolId;
    }

    @Override
    public Optional<String> findEmailByOperatorId(String operatorId) {
        if (operatorId == null || operatorId.isBlank()) {
            log.warn("operatorId is null or blank, cannot look up email");
            return Optional.empty();
        }
        try {
            AdminGetUserResponse response = cognito.adminGetUser(
                AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(operatorId)
                    .build()
            );
            return response.userAttributes().stream()
                .filter(attr -> "email".equals(attr.name()))
                .map(AttributeType::value)
                .findFirst();
        } catch (UserNotFoundException e) {
            log.warn("Operator {} not found in Cognito user pool {}", operatorId, userPoolId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error looking up email for operator {} in Cognito: {}", operatorId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
