package com.fulfillment.shippingservice.infrastructure.repository.memory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.shippingservice.domain.ports.ShippingGuideStorage;

@Component
@Profile("local")
public class InMemoryShippingGuideStorage implements ShippingGuideStorage {

    private final ConcurrentMap<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public String upload(String shipmentId, byte[] pdfBytes) {
        String key = "shipments/" + shipmentId + "/guide.pdf";
        store.put(key, pdfBytes);
        return key;
    }

    @Override
    public String getPresignedUrl(String s3Key, Duration expiry) {
        return "local://" + s3Key;
    }
}
