package org.example.controller;


import org.example.model.ApiError;
import org.example.model.Product;
import org.example.service.ProductService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EnableWebMvc
@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

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
        logger.info("GET /products called");
        return ResponseEntity.ok().headers(getCorsHeaders()).body(productService.getAllProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductById(@PathVariable String productId) {
        logger.info("GET /products/{} called", productId);
        Optional<Product> product = productService.getProductById(productId);

        if (product.isPresent()) {
            logger.info("POST /products called with data: {}", product);
            return ResponseEntity.ok().headers(getCorsHeaders()).body(product);
        } else {
            ApiError error = new ApiError(HttpStatus.NOT_FOUND, "Product with ID " + productId + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(getCorsHeaders()).body(error);
        }
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().headers(getCorsHeaders()).build();
    }


    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {

        if (product.title() == null || product.title().isEmpty() ||
            product.price() <= 0 || product.count() < 0) {

            return ResponseEntity.badRequest()
                    .headers(getCorsHeaders())
                    .body(new ApiError(HttpStatus.BAD_REQUEST, "Invalid product data: title, price, and count are required"));
        }

        Product createdProduct = productService.createProduct(
                product.title(),
                product.description(),
                product.price(),
                product.count()
        );
        return ResponseEntity.status(HttpStatus.CREATED).headers(getCorsHeaders()).body(createdProduct);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGlobalException(Exception ex) {
        logger.error("Error handling message: {}", ex.getMessage());
        ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(getCorsHeaders()).body(error);
    }

}

