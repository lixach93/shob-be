package org.example;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class StreamLambdaHandlerTest {

    private static StreamLambdaHandler handler;
    private static Context lambdaContext;

    @BeforeAll
    public static void setUp() {
        handler = new StreamLambdaHandler();
        lambdaContext = new MockLambdaContext();
    }

    @Test
    public void getProducts_ReturnsProductList() {
        InputStream requestStream = new AwsProxyRequestBuilder("/products", "GET")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .buildStream();
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        handle(requestStream, responseStream);

        AwsProxyResponse response = readResponse(responseStream);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertFalse(response.isBase64Encoded());
        assertTrue(response.getBody().contains("title"));
    }

    @Test
    public void getProductById_ReturnsCorrectProduct() {
        InputStream requestStream = new AwsProxyRequestBuilder("/products/39c4f230-3cda-4998-ac74-a3bb992af8aa", "GET")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .buildStream();
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        handle(requestStream, responseStream);

        AwsProxyResponse response = readResponse(responseStream);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"id\":\"39c4f230-3cda-4998-ac74-a3bb992af8aa\""));
        assertTrue(response.getBody().contains("\"title\""));
        assertTrue(response.getBody().contains("\"price\""));
        assertTrue(response.getBody().contains("\"count\""));
    }

    @Test
    public void getProductById_ProductNotFound_Returns404() {
        InputStream requestStream = new AwsProxyRequestBuilder("/products/99", "GET")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .buildStream();
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        handle(requestStream, responseStream);

        AwsProxyResponse response = readResponse(responseStream);
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Product with ID 99 not found"));
    }

    @Test
    public void createProduct_ValidData_Returns201() {
        String requestBody = """
            {
                "title": "New Product",
                "description": "Awesome product",
                "price": 100,
                "count": 10
            }
        """;

        InputStream requestStream = new AwsProxyRequestBuilder("/products", "POST")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .buildStream();
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        handle(requestStream, responseStream);

        AwsProxyResponse response = readResponse(responseStream);
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertTrue(response.getBody().contains("\"title\":\"New Product\""));
    }

    @Test
    public void createProduct_InvalidData_Returns400() {
        String requestBody = """
            {
                "title": "",
                "description": "Bad product",
                "price": -10,
                "count": -5
            }
        """;

        InputStream requestStream = new AwsProxyRequestBuilder("/products", "POST")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .buildStream();
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        handle(requestStream, responseStream);

        AwsProxyResponse response = readResponse(responseStream);
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid product data"));
    }


    @Test
    public void invalidResource_Returns404() {
        InputStream requestStream = new AwsProxyRequestBuilder("/invalid", "GET")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .buildStream();
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        handle(requestStream, responseStream);

        AwsProxyResponse response = readResponse(responseStream);
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
    }

    private void handle(InputStream is, ByteArrayOutputStream os) {
        try {
            handler.handleRequest(is, os, lambdaContext);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private AwsProxyResponse readResponse(ByteArrayOutputStream responseStream) {
        try {
            return LambdaContainerHandler.getObjectMapper().readValue(responseStream.toByteArray(), AwsProxyResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Error while parsing response: " + e.getMessage());
        }
        return null;
    }
}
