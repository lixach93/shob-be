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

        assertTrue(response.getBody().contains("Product 1"));
        assertTrue(response.getBody().contains("Product 5"));
        assertTrue(response.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertTrue(response.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE).startsWith(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    public void getProductById_ReturnsCorrectProduct() {
        InputStream requestStream = new AwsProxyRequestBuilder("/products/3", "GET")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .buildStream();
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        handle(requestStream, responseStream);

        AwsProxyResponse response = readResponse(responseStream);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"id\":3"));
        assertTrue(response.getBody().contains("\"title\":\"Product 3\""));
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
