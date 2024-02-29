import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class CrptApi{
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCount;
    private final Object lock = new Object();
    private long lastRequestTime;
    private static final String requestURL = "https://ismp.crpt.ru/api/v3/lk/documents/create";



    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
    }

    public void createDocument(Document document, String signature) throws IOException {
        synchronized (lock) {
            try {
                long currentTime = System.currentTimeMillis();
                int currentCount = requestCount.get();
                if (currentCount == 0 || currentTime - lastRequestTime >= timeUnit.toMillis(1)) {
                    lastRequestTime = currentTime;
                    requestCount.set(1);
                } else if (currentCount < requestLimit) {
                    requestCount.incrementAndGet();
                } else {
                    long sleepTime = lastRequestTime + timeUnit.toMillis(1) - currentTime;
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                    lastRequestTime = System.currentTimeMillis();
                    requestCount.set(1);
                }

                CloseableHttpClient httpClient = HttpClients.createDefault();
                ObjectMapper mapper = new ObjectMapper();
                String newJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
                HttpPost post = new HttpPost(requestURL);
                post.setHeader("Content-Type", "application/json");
                post.setHeader("Signature", signature);
                HttpEntity httpEntity = new StringEntity(newJson, ContentType.APPLICATION_JSON);
                post.setEntity(httpEntity);
                try (CloseableHttpResponse response = httpClient.execute(post)) {
                    HttpEntity responseEntity = response.getEntity();
                    String responseJson = EntityUtils.toString(responseEntity);
                    System.out.println(responseJson); //
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS,6);
        ObjectMapper mapper = new ObjectMapper();
        String signature = "signature";
        Document document = mapper.readValue(GetJson.js, Document.class);
        crptApi.createDocument(document,signature);


    }




    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;
    }
    @Getter
    @Setter
    static class Description {
        private String participantInn;
    }
    @Getter
    @Setter
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
     public  static class GetJson {
        private static  final String js = """
                {"description":
                { "participantInn": "string" }, "doc_id": "string", "doc_status": "string",
                  "doc_type": "LP_INTRODUCE_GOODS", "importRequest": true,
                  "owner_inn": "string", "participant_inn": "string", "producer_inn":
                "string", "production_date": "2020-01-23", "production_type": "string",
                  "products": [ { "certificate_document": "string",
                    "certificate_document_date": "2020-01-23",
                    "certificate_document_number": "string", "owner_inn": "string",
                    "producer_inn": "string", "production_date": "2020-01-23",
                    "tnved_code": "string", "uit_code": "string", "uitu_code": "string" } ],
                  "reg_date": "2020-01-23", "reg_number": "string"}""";
    }


}



