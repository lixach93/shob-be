package org.example;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class App {
    public static void main(String[] args) {

        DynamoDbClient dynamoDb = DynamoDbClient.create();

        String productsTable = "products";
        String stocksTable = "stocks";

        List<Map<String, AttributeValue>> testProducts = List.of(
                createProduct("Laptop", "High performance laptop", 1200),
                createProduct("Smartphone", "Latest model smartphone", 800),
                createProduct("Headphones", "Noise-cancelling headphones", 200),
                createProduct("Monitor", "4K Ultra HD monitor", 600),
                createProduct("Keyboard", "Mechanical keyboard", 150)
        );

        for (Map<String, AttributeValue> product : testProducts) {
            if (!product.containsKey("id")) {
                System.out.println("❌ ERROR: Product is missing 'id' key!");
                continue;
            }

            System.out.println("DEBUG: Product item before inserting → " + product);

            HashMap<String, AttributeValue> item = new HashMap<>(product);

            PutItemRequest putProduct = PutItemRequest.builder()
                    .tableName(productsTable)
                    .item(item)
                    .build();

            dynamoDb.putItem(putProduct);

            String productId = product.get("id").s();

            addStock(dynamoDb, productId, 10);

            System.out.println("✅ Added product: " + product.get("title").s() + " (ID: " + productId + ")");
        }

        System.out.println("✅ Data successfully inserted into DynamoDB!");
        dynamoDb.close();
    }

    private static Map<String, AttributeValue> createProduct(String title, String description, int price) {
        String uuid = UUID.randomUUID().toString();
        HashMap<String, AttributeValue> product = new HashMap<>();
        product.put("id", AttributeValue.builder().s(uuid).build()); // ✅ Теперь всегда есть ключ "id"
        product.put("title", AttributeValue.builder().s(title).build());
        product.put("description", AttributeValue.builder().s(description).build());
        product.put("price", AttributeValue.builder().n(String.valueOf(price)).build());
        return product;
    }

    private static void addStock(DynamoDbClient dynamoDb, String productId, int count) {
        if (!productExists(dynamoDb, productId)) {
            System.out.println("❌ ERROR: Product with ID " + productId + " not found in 'products' table!");
            return;
        }

        HashMap<String, AttributeValue> stockItem = new HashMap<>();
        stockItem.put("product_id", AttributeValue.builder().s(productId).build());
        stockItem.put("count", AttributeValue.builder().n(String.valueOf(count)).build());

        PutItemRequest putStock = PutItemRequest.builder()
                .tableName("stocks")
                .item(stockItem) // ✅ Передаём HashMap
                .build();

        dynamoDb.putItem(putStock);
        System.out.println("✅ Added stock for product: " + productId);
    }

    private static boolean productExists(DynamoDbClient dynamoDb, String productId) {
        GetItemRequest getRequest = GetItemRequest.builder()
                .tableName("products")
                .key(Map.of("id", AttributeValue.builder().s(productId).build()))
                .build();

        return dynamoDb.getItem(getRequest).hasItem();
    }
}
