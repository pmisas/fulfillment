package com.fulfillment.shippingservice.infrastructure.s3;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.shippingservice.domain.ports.ShippingGuideStorage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Component
@Profile("cloud")
public class S3ShippingGuideStorage implements ShippingGuideStorage {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3ShippingGuideStorage(
            S3Client s3Client,
            S3Presigner presigner,
            @Value("${aws.s3.shipping-guides-bucket}") String bucket) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    @Override
    public String upload(String shipmentId, byte[] pdfBytes) {
        String key = "shipments/" + shipmentId + "/guide.pdf";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("application/pdf")
                        .contentLength((long) pdfBytes.length)
                        .build(),
                RequestBody.fromBytes(pdfBytes));
        return key;
    }

    @Override
    public String getPresignedUrl(String s3Key, Duration expiry) {
        PresignedGetObjectRequest presigned = presigner.presignGetObject(r -> r
                .signatureDuration(expiry)
                .getObjectRequest(b -> b.bucket(bucket).key(s3Key)));
        return presigned.url().toString();
    }
}
