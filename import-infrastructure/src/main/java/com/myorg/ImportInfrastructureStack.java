package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.IResource;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MockIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.S3EventSource;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.CorsRule;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.s3.NotificationKeyFilter;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ImportInfrastructureStack extends Stack {
    public ImportInfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // 🔹 Генерируем случайное имя для S3 бакета (каждый деплой создаёт новый бакет)
        String uniqueBucketName = "import-bucket-" + UUID.randomUUID().toString().substring(0, 8);
        Bucket importBucket = Bucket.Builder.create(this, "ImportBucket")
                .bucketName(uniqueBucketName)
                .removalPolicy(software.amazon.awscdk.RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .cors(List.of(
                        CorsRule.builder()
                                .allowedOrigins(List.of("*"))  // ✅ Разрешаем запросы с любого домена
                                .allowedMethods(List.of(
                                        software.amazon.awscdk.services.s3.HttpMethods.GET,
                                        software.amazon.awscdk.services.s3.HttpMethods.PUT,
                                        software.amazon.awscdk.services.s3.HttpMethods.POST,
                                        software.amazon.awscdk.services.s3.HttpMethods.HEAD
                                ))  // ✅ Разрешаем `PUT`, `POST`, `GET`, `HEAD`
                                .allowedHeaders(List.of("*"))  // ✅ Разрешаем все заголовки
                                .exposedHeaders(List.of("ETag"))  // ✅ Позволяем читать заголовки (например, ETag)
                                .build()
                ))
                .build();

        // 🔹 Создаём IAM-роль для Lambda с доступом к S3
        Role lambdaRole = Role.Builder.create(this, "ImportLambdaExecutionRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"),
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess")
                ))
                .build();

        // 🔹 Лямбда importProductsFile (генерирует Signed URL)
        Function importProductsFileLambda = Function.Builder.create(this, "ImportProductsFileLambda")
                .runtime(Runtime.JAVA_17)
                .handler("org.example.StreamLambdaHandler::handleRequest") // Запуск Spring Boot
                .code(Code.fromAsset("../import-service/target/import-service-1.0-SNAPSHOT-lambda-package.zip"))
                .memorySize(1024)
                .timeout(Duration.seconds(20))
                .functionName("importProductsFile")
                .role(lambdaRole)
                .environment(Map.of(
                        "IMPORT_BUCKET_NAME", uniqueBucketName // Передаём имя бакета
                ))
                .build();

        // 🔹 Лямбда importFileParser (обрабатывает S3-события)
        Function importFileParserLambda = Function.Builder.create(this, "ImportFileParserLambda")
                .runtime(Runtime.JAVA_17)
                .handler("org.example.service.ImportFileParserHandler::handleRequest")
                .code(Code.fromAsset("../import-service/target/import-service-1.0-SNAPSHOT-lambda-package.zip"))
                .memorySize(512)
                .timeout(Duration.seconds(10))
                .functionName("importFileParser")
                .role(lambdaRole)
                .environment(Map.of(
                        "IMPORT_BUCKET_NAME", uniqueBucketName
                ))
                .build();

        // 🔹 Подписка importFileParser на S3 события (обрабатывает только файлы в uploaded/)
        importFileParserLambda.addEventSource(S3EventSource.Builder.create(importBucket)
                .events(List.of(EventType.OBJECT_CREATED))
                .filters(List.of(NotificationKeyFilter.builder().prefix("uploaded/").build())) // ✅ Фильтр для папки uploaded/
                .build());

        // 🔹 Создаём API Gateway с CORS
        RestApi api = RestApi.Builder.create(this, "ImportServiceAPI")
                .restApiName("Import Service API")
                .defaultCorsPreflightOptions(software.amazon.awscdk.services.apigateway.CorsOptions.builder()
                        .allowOrigins(List.of("*"))  // ✅ Разрешаем запросы с любого домена (Фронт)
                        .allowMethods(List.of("GET", "OPTIONS")) // ✅ Разрешаем методы GET и OPTIONS
                        .allowHeaders(List.of("Content-Type", "Authorization")) // ✅ Разрешаем нужные заголовки
                        .build())
                .build();

        // 🔹 Создаём ресурс /import
        IResource importResource = api.getRoot().addResource("import");


        // 🔹 Подключаем Lambda importProductsFile к API Gateway
        importResource.addMethod("GET", LambdaIntegration.Builder.create(importProductsFileLambda).build());
    }
}
