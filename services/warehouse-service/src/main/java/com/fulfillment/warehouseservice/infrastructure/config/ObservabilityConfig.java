package com.fulfillment.warehouseservice.infrastructure.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.amazonaws.xray.plugins.ECSPlugin;
import com.amazonaws.xray.plugins.EKSPlugin;
import com.amazonaws.xray.strategy.jakarta.SegmentNamingStrategy;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import jakarta.servlet.Filter;
import java.time.Duration;
import java.util.Map;

/**
 * Configuration for CloudWatch metrics and AWS X-Ray distributed tracing
 */
@Configuration
public class ObservabilityConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${aws.xray.enabled:true}")
    private boolean xrayEnabled;

    /**
     * Configure AWS X-Ray for distributed tracing
     */
    @Bean
    public Filter tracingFilter() {
        if (xrayEnabled) {
            // Configure X-Ray recorder with AWS plugins
            AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
                    .withContextMissingStrategy(new com.amazonaws.xray.strategy.LogErrorContextMissingStrategy())
                    .withPlugin(new EC2Plugin())
                    .withPlugin(new ECSPlugin())
                    .withPlugin(new EKSPlugin());

            AWSXRay.setGlobalRecorder(builder.build());

            // Return servlet filter for automatic tracing
            return new AWSXRayServletFilter(
                    SegmentNamingStrategy.dynamic(applicationName)
            );
        }
        
        return (request, response, chain) -> chain.doFilter(request, response);
    }

    /**
     * Configure CloudWatch Metrics Registry
     */
    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public CloudWatchConfig cloudWatchConfig() {
        return new CloudWatchConfig() {
            private final Map<String, String> configuration = Map.of(
                    "cloudwatch.namespace", "Fulfillment/WarehouseService",
                    "cloudwatch.step", Duration.ofMinutes(1).toString()
            );

            @Override
            public String get(String key) {
                return configuration.get(key);
            }
        };
    }

    @Bean
    public MeterRegistry cloudWatchMeterRegistry(
            CloudWatchConfig cloudWatchConfig,
            CloudWatchAsyncClient cloudWatchAsyncClient) {
        return new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, cloudWatchAsyncClient);
    }

    /**
     * Add common tags to all metrics
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name}") String applicationName,
            @Value("${ENVIRONMENT:local}") String environment) {
        return registry -> registry.config()
                .commonTags(
                        "application", applicationName,
                        "environment", environment,
                        "region", awsRegion
                );
    }
}
