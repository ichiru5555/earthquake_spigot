package org.ichiru.earthquake;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpClientUtil {
    private final HttpClient httpClient;

    public HttpClientUtil() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public void sendAsyncGetRequest(String url, HttpResponse.BodyHandler<String> responseHandler) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .build();

        httpClient.sendAsync(request, responseHandler)
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    // The response processing logic should be implemented here or passed via callbacks
                    System.out.println("Response received: " + response);
                });
    }

}
