package org.example.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.example.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ImportFileParserHandler {
    private static final Logger logger = LoggerFactory.getLogger(ImportFileParserHandler.class);

    private final S3Client s3Client;  // Используем AWS SDK v2


    // 🔹 Конструктор по умолчанию (используется в AWS Lambda)
    public ImportFileParserHandler() {
        this.s3Client = S3Client.create();
    }

    // 🔹 Конструктор для тестов (позволяет передавать мокнутый S3Client)
    public ImportFileParserHandler(S3Client s3Client) {
        this.s3Client = s3Client;
    }
    public void handleRequest(S3Event event, Context context) {
        event.getRecords().forEach(record -> {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();

            logger.info("Processing file from S3: {}/{}", bucket, key);

            try {
                // 🔹 Загружаем CSV-файл из S3
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

                ResponseInputStream<GetObjectResponse> objectStream = s3Client.getObject(getObjectRequest);

                // 🔹 Читаем CSV-файл и парсим его в Product-объекты
                List<Product> products = new ArrayList<>();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(objectStream));
                     CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

                    for (CSVRecord csvRecord : parser) {
                        try {
                            Product product = new Product(
                                    csvRecord.get("id"),
                                    csvRecord.get("title"),
                                    csvRecord.get("description"),
                                    Integer.parseInt(csvRecord.get("price")),
                                    Integer.parseInt(csvRecord.get("count"))
                            );
                            products.add(product);
                        } catch (Exception e) {
                            logger.error("Error parsing record: {}", csvRecord.toMap(), e);
                        }
                    }
                }

                // 🔹 Логируем успешно загруженные продукты
                products.forEach(product -> logger.info("Parsed product: {}", product));

                // 🔹 Перемещаем файл в `parsed/`
                moveFileToParsed(bucket, key);

            } catch (Exception e) {
                logger.error("Error processing S3 event for file: {}/{}", bucket, key, e);
            }
        });
    }

    private void moveFileToParsed(String bucket, String key) {
        try {
            // 🔹 Генерируем новый путь `parsed/`
            String parsedKey = key.replace("uploaded/", "parsed/");

            // 🔹 Копируем файл в `parsed/`
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(key)
                    .destinationBucket(bucket)
                    .destinationKey(parsedKey)
                    .build();

            s3Client.copyObject(copyRequest);
            logger.info("File copied to: {}/{}", bucket, parsedKey);

            // 🔹 Удаляем оригинальный файл
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            logger.info("Original file deleted from: {}/{}", bucket, key);

        } catch (Exception e) {
            logger.error("Error moving file to parsed folder: {}/{}", bucket, key, e);
        }
    }
}
