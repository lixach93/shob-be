package org.example.controller;


import org.example.model.ApiError;
import org.example.model.Product;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Optional;

@EnableWebMvc
@RestController
@RequestMapping("/products")
public class ProductController {

    private final List<Product> products = List.of(
            new Product(1, "100.0", "Product 1"),
            new Product(2, "200.0", "Product 2"),
            new Product(3, "300.0", "Product 3"),
            new Product(4, "400.0", "Product 4"),
            new Product(5, "500.0", "Product 5")
    );

    private HttpHeaders getCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Api-Key, X-Amz-Date, X-Amz-Security-Token");
        headers.add("Access-Control-Allow-Credentials", "true");
        return headers;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok().headers(getCorsHeaders()).body(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductById(@PathVariable int productId) {
        Optional<Product> product = products.stream()
                .filter(p -> p.id() == productId)
                .findFirst();

        if (product.isPresent()) {
            return ResponseEntity.ok().headers(getCorsHeaders()).body(product.get());
        } else {
            ApiError error = new ApiError(HttpStatus.NOT_FOUND, "Product with ID " + productId + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(getCorsHeaders()).body(error);
        }
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().headers(getCorsHeaders()).build();
    }
}

