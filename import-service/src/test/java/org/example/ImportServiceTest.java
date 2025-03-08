package org.example;

import org.example.service.ImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportServiceTest {
    private ImportService importService;

    @Mock
    private S3Presigner mockPresigner;

    @Mock
    private PresignedPutObjectRequest mockPresignedRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        importService = new ImportService(mockPresigner, "test-bucket");
    }

    @Test
    void testGenerateSignedUrl() throws MalformedURLException {
        String testFileName = "test.csv";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/uploaded/" + testFileName;

        // 🔹 Мокаем поведение PresignedPutObjectRequest
        when(mockPresignedRequest.url()).thenReturn(new URL(expectedUrl)); // ✅ Используем URL вместо URI

        // 🔹 Мокаем `S3Presigner`
        when(mockPresigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(mockPresignedRequest);

        // 🔹 Вызываем метод
        String signedUrl = importService.generateSignedUrl(testFileName);

        // 🔹 Проверяем, что URL не пустой
        assertNotNull(signedUrl);
        assertTrue(signedUrl.contains("uploaded/" + testFileName));
        assertTrue(signedUrl.startsWith("https://test-bucket.s3.amazonaws.com/"));

        // 🔹 Проверяем, что presigner был вызван
        verify(mockPresigner, times(1)).presignPutObject(any(PutObjectPresignRequest.class));
    }
}
