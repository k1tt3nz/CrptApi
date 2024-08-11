package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.semaphore = new Semaphore(requestLimit, true);
        this.scheduler = Executors.newScheduledThreadPool(1);
        long timeIntervalMillis = timeUnit.toMillis(1);
        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()),
                timeIntervalMillis, timeIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public HttpResponse<String> createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();

        String jsonBody = objectMapper.writeValueAsString(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public static class Document {
        public Description description;
        public String docId;
        public String docStatus;
        public String docType;
        public boolean importRequest;
        public String ownerInn;
        public String participantInn;
        public String producerInn;
        public String productionDate;
        public String productionType;
        public Product[] products;
        public String regDate;
        public String regNumber;

        public static class Product {
            public String certificateDocument;
            public String certificateDocumentDate;
            public String certificateDocumentNumber;
            public String ownerInn;
            public String producerInn;
            public String productionDate;
            public String tnvedCode;
            public String uitCode;
            public String uituCode;
        }
    }

    public static class Description {
        public String participantInn;
    }

    public static void main(String[] args) {

        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);

        CrptApi.Document document = new CrptApi.Document();
        document.description = new CrptApi.Description();
        document.description.participantInn = "0987654321";
        document.docId = "123456";
        document.docStatus = "DRAFT";
        document.docType = "LP_INTRODUCE_GOODS";
        document.importRequest = true;
        document.ownerInn = "1234567890";
        document.participantInn = "0987654321";
        document.producerInn = "1122334455";
        document.productionDate = "2023-01-23";
        document.productionType = "MASS_PRODUCTION";
        document.regDate = "2024-08-11";
        document.regNumber = "REG123";

        CrptApi.Document.Product product = new CrptApi.Document.Product();
        product.certificateDocument = "CERT123";
        product.certificateDocumentDate = "2024-01-23";
        product.certificateDocumentNumber = "CERTNUM123";
        product.ownerInn = "1234567890";
        product.producerInn = "1122334455";
        product.productionDate = "2024-01-23";
        product.tnvedCode = "12345678";
        product.uitCode = "UIT123";
        product.uituCode = "UITU123";

        document.products = new CrptApi.Document.Product[]{product};

        String signature = "ExampleSignature";

        try {
            HttpResponse<String> response = crptApi.createDocument(document, signature);

            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            crptApi.shutdown();
        }
    }
}