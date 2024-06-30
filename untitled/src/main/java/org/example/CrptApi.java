package org.example;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {

    private final Semaphore semaphore;
    private final long intervalMillis;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Конструктор класса
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.intervalMillis = timeUnit.toMillis(1);  // Промежуток времени, за который разрешено определённое количество запросов
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();

        // Запуск и сброс лимита
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                semaphore.release(requestLimit - semaphore.availablePermits());
            }
        }).start();
    }

    // Основной метод для создания документа
    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();

        HttpPost post = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Signature", signature);

        String json = objectMapper.writeValueAsString(document);
        post.setEntity(new StringEntity(json));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            if (response.getCode() != 200) {
                throw new RuntimeException("Failed: HTTP error code : " + response.getCode());
            }
        }
    }

    // Внутренний класс-сущность документа
    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }
}