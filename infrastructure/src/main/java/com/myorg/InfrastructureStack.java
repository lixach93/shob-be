package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;


public class InfrastructureStack extends Stack {

    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Role lambdaRole = Role.Builder.create(this, "LambdaExecutionRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole")
                ))
                .build();

        Function getProductsList = Function.Builder.create(this, "GetProductsListLambda")
                .runtime(Runtime.JAVA_17)
                .handler("org.example.StreamLambdaHandler::handleRequest")
                .code(Code.fromAsset("../product-service/target/product-service-1.0-SNAPSHOT-lambda-package.zip"))
                .memorySize(512)
                .timeout(Duration.seconds(10))
                .functionName("getProductsList")
                .role(lambdaRole)
                .environment(Map.of(
                        "ACCESS_CONTROL_ALLOW_ORIGIN", "*",
                        "ACCESS_CONTROL_ALLOW_METHODS", "GET, OPTIONS",
                        "ACCESS_CONTROL_ALLOW_HEADERS", "Content-Type, Authorization"
                ))
                .build();

        Function getProductById = Function.Builder.create(this, "GetProductByIdLambda")
                .runtime(Runtime.JAVA_17)
                .handler("org.example.StreamLambdaHandler::handleRequest")
                .code(Code.fromAsset("../product-service/target/product-service-1.0-SNAPSHOT-lambda-package.zip"))
                .memorySize(512)
                .timeout(Duration.seconds(10))
                .functionName("getProductById")
                .role(lambdaRole)
                .environment(Map.of(
                        "ACCESS_CONTROL_ALLOW_ORIGIN", "*",
                        "ACCESS_CONTROL_ALLOW_METHODS", "GET, OPTIONS",
                        "ACCESS_CONTROL_ALLOW_HEADERS", "Content-Type, Authorization"
                ))
                .build();

        RestApi api = RestApi.Builder.create(this, "ProductServiceAPI")
                .restApiName("Product Service API")
                .build();

        api.getRoot().addCorsPreflight(CorsOptions.builder()
                .allowOrigins(List.of("*"))  // Разрешить все домены
                .allowMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")) // Разрешённые методы
                .allowHeaders(List.of("Content-Type", "Authorization", "X-Amz-Date", "X-Api-Key", "X-Amz-Security-Token", "X-Amz-User-Agent"))
                .allowCredentials(true)
                .build());

        IResource products = api.getRoot().addResource("products");

        products.addCorsPreflight(CorsOptions.builder()
                .allowOrigins(List.of("*"))
                .allowMethods(List.of("GET", "OPTIONS"))
                .allowHeaders(List.of("Content-Type", "Authorization"))
                .build());

        products.addMethod("GET", LambdaIntegration.Builder.create(getProductsList).build());

        IResource productById = products.addResource("{productId}");

        productById.addCorsPreflight(CorsOptions.builder()
                .allowOrigins(List.of("*"))
                .allowMethods(List.of("GET", "OPTIONS"))
                .allowHeaders(List.of("Content-Type", "Authorization"))
                .build());

        productById.addMethod("GET", LambdaIntegration.Builder.create(getProductById).build());
    }
}
