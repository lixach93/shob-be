package org.example;

import org.example.controller.ImportController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;


@SpringBootApplication
// We use direct @Import instead of @ComponentScan to speed up cold starts
// @ComponentScan(basePackages = "org.example.controller")
@Import({ ImportController.class })
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}