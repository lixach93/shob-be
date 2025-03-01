package org.example.service;

import org.example.model.Product;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final DynamoDbClient dynamoDb;
    private final String productsTable;
    private final String stocksTable;

    public ProductService() {
        this.dynamoDb = DynamoDbClient.create();
        String productsTableName = System.getenv("PRODUCTS_TABLE_NAME");
        String stocksTableName = System.getenv("STOCKS_TABLE_NAME");
        String products = Optional.ofNullable(productsTableName).orElse("products");
        String stocks = Optional.ofNullable(stocksTableName).orElse("stocks");
        this.productsTable = products;
        this.stocksTable = stocks;
    }

    public List<Product> getAllProducts() {
        ScanRequest scanProducts = ScanRequest.builder().tableName(productsTable).build();
        ScanResponse productsResponse = dynamoDb.scan(scanProducts);

        ScanRequest scanStocks = ScanRequest.builder().tableName(stocksTable).build();
        ScanResponse stocksResponse = dynamoDb.scan(scanStocks);

        Map<String, Integer> stockMap = stocksResponse.items().stream()
                .collect(Collectors.toMap(
                        item -> item.get("product_id").s(),
                        item -> Integer.parseInt(item.get("count").n())
                ));

        List<Product> products = new ArrayList<>();
        for (Map<String, AttributeValue> productItem : productsResponse.items()) {
            String productId = productItem.get("id").s();
            products.add(new Product(
                    productId,
                    productItem.get("title").s(),
                    productItem.get("description").s(),
                    Integer.parseInt(productItem.get("price").n()),
                    stockMap.getOrDefault(productId, 0)
            ));
        }

        return products;
    }

    public Optional<Product> getProductById(String productId) {
        GetItemRequest productRequest = GetItemRequest.builder()
                .tableName(productsTable)
                .key(Map.of("id", AttributeValue.builder().s(productId).build()))
                .build();

        Map<String, AttributeValue> productItem = dynamoDb.getItem(productRequest).item();
        if (productItem.isEmpty()) {
            return Optional.empty();
        }

        GetItemRequest stockRequest = GetItemRequest.builder()
                .tableName(stocksTable)
                .key(Map.of("product_id", AttributeValue.builder().s(productId).build()))
                .build();

        Map<String, AttributeValue> stockItem = dynamoDb.getItem(stockRequest).item();
        int count = stockItem.isEmpty() ? 0 : Integer.parseInt(stockItem.get("count").n());

        Product product = new Product(
                productId,
                productItem.get("title").s(),
                productItem.get("description").s(),
                Integer.parseInt(productItem.get("price").n()),
                count
        );

        return Optional.of(product);
    }

    public Product createProduct(String title, String description, int price, int count) {
        String productId = UUID.randomUUID().toString(); // Генерируем UUID

        Map<String, AttributeValue> productItem = Map.of(
                "id", AttributeValue.builder().s(productId).build(),
                "title", AttributeValue.builder().s(title).build(),
                "description", AttributeValue.builder().s(description).build(),
                "price", AttributeValue.builder().n(String.valueOf(price)).build()
        );

        Map<String, AttributeValue> stockItem = Map.of(
                "product_id", AttributeValue.builder().s(productId).build(),
                "count", AttributeValue.builder().n(String.valueOf(count)).build()
        );

        TransactWriteItemsRequest transactWriteItemsRequest = TransactWriteItemsRequest.builder()
                .transactItems(List.of(
                        TransactWriteItem.builder().put(
                                Put.builder().tableName(productsTable).item(productItem).build()
                        ).build(),
                        TransactWriteItem.builder().put(
                                Put.builder().tableName(stocksTable).item(stockItem).build()
                        ).build()
                ))
                .build();

        try {
            dynamoDb.transactWriteItems(transactWriteItemsRequest);
            return new Product(productId, title, description, price, count);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create product transactionally: " + e.getMessage());
        }
    }
}
