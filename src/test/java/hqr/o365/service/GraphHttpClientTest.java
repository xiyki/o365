package hqr.o365.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GraphHttpClientTest {
    @Test
    void getRetriesOnceWhenGraphSuppliesShortRetryAfter() {
        RestTemplate client = GraphHttpClient.create();
        MockRestServiceServer server = MockRestServiceServer.createServer(client);
        String url = "https://graph.microsoft.com/v1.0/users";
        HttpHeaders retryHeaders = new HttpHeaders();
        retryHeaders.add("Retry-After", "0");
        server.expect(once(), requestTo(url))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryHeaders));
        server.expect(once(), requestTo(url))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertEquals("{}", client.getForObject(url, String.class));
        server.verify();
    }
}
