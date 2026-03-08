package com.fulfillment.shippingservice.domain.ports;

import java.time.Duration;

public interface ShippingGuideStorage {
    String upload(String shipmentId, byte[] pdfBytes);
    String getPresignedUrl(String s3Key, Duration expiry);
}
