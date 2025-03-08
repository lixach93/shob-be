package org.example.controller;

import org.example.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/import")
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public ResponseEntity<String> importProductsFile(@RequestParam String name) {
        logger.info("Received request to import products file");
        String signedUrl = importService.generateSignedUrl(name);
        return ResponseEntity.ok(signedUrl);
    }

}
