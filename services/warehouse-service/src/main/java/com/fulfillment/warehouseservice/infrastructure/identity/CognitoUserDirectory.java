package com.fulfillment.warehouseservice.infrastructure.identity;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.warehouseservice.domain.port.UserDirectory;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

@Component
@Profile("cloud")
public class CognitoUserDirectory implements UserDirectory {

    private static final Logger log = LoggerFactory.getLogger(CognitoUserDirectory.class);

    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolId;

    public CognitoUserDirectory(
            CognitoIdentityProviderClient cognitoClient,
            @Value("${aws.cognito.user-pool-id}") String userPoolId) {
        this.cognitoClient = cognitoClient;
        this.userPoolId = userPoolId;
    }

    @Override
    public Optional<DirectoryUser> findById(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        try {
            Optional<UserType> user = cognitoClient.listUsers(ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .filter("sub = \"" + escape(userId.trim()) + "\"")
                    .limit(1)
                    .build())
                .users()
                .stream()
                .findFirst();

            if (user.isEmpty()) {
                return Optional.empty();
            }

            String username = user.get().username();
            Set<String> roles = cognitoClient.adminListGroupsForUser(AdminListGroupsForUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build())
                .groups()
                .stream()
                .map(group -> group.groupName())
                .collect(Collectors.toSet());

            return Optional.of(new DirectoryUser(userId.trim(), username, roles));
        } catch (Exception ex) {
            log.error("Failed to resolve user {} in Cognito", userId, ex);
            throw new IllegalStateException("Failed to validate user in Cognito", ex);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
