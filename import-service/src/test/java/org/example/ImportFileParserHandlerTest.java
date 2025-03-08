package org.example.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportFileParserHandlerTest {
    private ImportFileParserHandler handler;

    @Mock
    private S3Client mockS3Client;

    @Mock
    private Context mockContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new ImportFileParserHandler(mockS3Client); // ✅ Передаём мокнутый S3Client
    }

    @Test
    void testHandleRequest() throws Exception {
        // 🔹 Подготавливаем CSV-контент
        String csvContent = "id,title,description,price,count\n" +
                            "1,Product A,Desc A,10,100\n" +
                            "2,Product B,Desc B,20,50\n";

        // 🔹 Создаём InputStream с CSV-контентом
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> responseInputStream = mock(ResponseInputStream.class);

        // 🔹 Мокаем поведение S3Client
        when(responseInputStream.readAllBytes()).thenReturn(csvContent.getBytes(StandardCharsets.UTF_8));
        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        // 🔹 Создаём мокнутый S3 Event с реальными данными
        S3Event s3Event = createS3Event("test-bucket", "uploaded/test.csv");

        // 🔹 Вызываем обработчик
        handler.handleRequest(s3Event, mockContext);

        // 🔹 Проверяем, что `S3Client.getObject()` был вызван 1 раз
        verify(mockS3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    /**
     * Метод для создания мокнутого `S3Event`
     */
    private S3Event createS3Event(String bucketName, String objectKey) {
        S3EventNotification.S3Entity s3Entity = new S3EventNotification.S3Entity(
                "configurationId",
                new S3EventNotification.S3BucketEntity(bucketName, new S3EventNotification.UserIdentityEntity("owner"), "arn:aws:s3:::" + bucketName),
                new S3EventNotification.S3ObjectEntity(objectKey, 1024L, "eTag", "versionId", "seq"),
                "s3SchemaVersion"
        );

        S3EventNotification.S3EventNotificationRecord record = new S3EventNotification.S3EventNotificationRecord(
                "us-east-1",
                "ObjectCreated:Put",
                "aws:s3",
                "2025-03-08T12:34:56.789Z",
                "1234567890",
                new S3EventNotification.RequestParametersEntity("127.0.0.1"),
                new S3EventNotification.ResponseElementsEntity("requestId", "hostId"),
                s3Entity,
                new S3EventNotification.UserIdentityEntity("principalId")
        );

        return new S3Event(Collections.singletonList(record));
    }
}
