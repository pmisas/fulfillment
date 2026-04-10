package com.fulfillment.inventoryservice.resilience;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.inventoryservice.infraestrcture.rest.GlobalExceptionHandler;

class InventoryGlobalExceptionHandlerResilienceTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new FailingDependencyController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @ParameterizedTest
    @CsvSource({
        "dynamodb,DynamoDB unavailable",
        "warehouse,Warehouse service unavailable",
        "transaction,Inventory reservation transaction failed",
        "authorization,Warehouse access lookup failed"
    })
    void shouldReturnInternalErrorResponse_whenUnexpectedDependencyFails(
            String failure,
            String expectedMessage) throws Exception {
        mockMvc.perform(get("/test/fail/" + failure))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").value(expectedMessage))
            .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @RestController
    private static class FailingDependencyController {
        @GetMapping("/test/fail/{failure}")
        void fail(@PathVariable String failure) {
            throw new RuntimeException(switch (failure) {
                case "dynamodb" -> "DynamoDB unavailable";
                case "warehouse" -> "Warehouse service unavailable";
                case "transaction" -> "Inventory reservation transaction failed";
                case "authorization" -> "Warehouse access lookup failed";
                default -> "Unexpected dependency failure";
            });
        }
    }
}
