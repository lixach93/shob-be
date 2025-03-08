package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.Duration;

@Service
public class ImportService {
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);
    private  S3Presigner s3Presigner;
    private  String bucketName;

    // 🔹 Конструктор для AWS Lambda (используется в реальном сервисе)
    public ImportService() {
        logger.info("Initializing S3Presigner");
        this.s3Presigner = S3Presigner.create();
        this.bucketName = System.getenv("IMPORT_BUCKET_NAME");
    }

    // 🔹 Конструктор для тестов (можно передать мокнутый S3Presigner)
    public ImportService(S3Presigner s3Presigner, String bucketName) {
        logger.info("Initializing S3Presigner test");
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    public String generateSignedUrl(String fileName) {
        logger.info("Generating Signed URL for file: {}", fileName);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("uploaded/" + fileName)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // URL действует 10 минут
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String signedUrl = presignedRequest.url().toString();
        logger.info("Generated Signed URL: {}", signedUrl);

        return signedUrl;
    }
}
